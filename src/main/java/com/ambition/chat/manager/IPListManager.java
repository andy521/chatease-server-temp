package com.ambition.chat.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ambition.chat.common.Punishment;
import com.ambition.chat.events.ErrorEvent;
import com.ambition.chat.events.Event;
import com.ambition.chat.events.EventListener;
import com.ambition.chat.events.LoaderEvent;
import com.ambition.chat.net.HttpLoader;
import com.ambition.chat.utils.Utils;

public class IPListManager {

	private static final Logger logger;
	private static final String IPLIST_REQ_URL;
	private static final Map<String, Punishment> blacklist;
	private static final List<String> whitelist;
	
	private static final HttpLoader loader;
	
	static {
		logger = LogManager.getLogger(IPListManager.class);
		IPLIST_REQ_URL = "http://localhost/websocket/data/iplist.json";
		blacklist = new HashMap<>();
		whitelist = new ArrayList<>();
		
		loader = new HttpLoader();
		loader.addEventListener(Event.COMPLETE, new EventListener() {
			
			@Override
			public void callback(Event e) {
				LoaderEvent evt = (LoaderEvent) e;
				JSONObject list = Utils.parse(evt.response());
				if (list == null) {
					return;
				}
				if (list.has("blacklist")) {
					parseBlacklist(list.getJSONArray("blacklist"));
				}
				if (list.has("whitelist")) {
					parseWhitelist(list.getJSONArray("whitelist"));
				}
				
				logger.warn("Getting IP list completed.");
			}
		});
		loader.addEventListener(Event.ERROR, new EventListener() {
			
			@Override
			public void callback(Event e) {
				ErrorEvent evt = (ErrorEvent) e;
				logger.error("Failed to load IP list. Explain: " + evt.explain());
			}
		});
		loader.addEventListener(LoaderEvent.CANCEL, new EventListener() {
			
			@Override
			public void callback(Event e) {
				logger.error("Loading IP list cancelled.");
			}
		});
	}
	
	public static void load() {
		loader.load(IPLIST_REQ_URL, null, null);
	}
	
	public static void syncLoad() {
		loader.syncLoad(IPLIST_REQ_URL, null, null);
	}
	
	private static void parseBlacklist(JSONArray list) {
		for (int i = 0; i < list.length(); i++) {
			JSONObject item = list.getJSONObject(i);
			if (item.has("ip") == false || item.has("punishment") == false) {
				continue;
			}
			
			String ip = item.getString("ip");
			
			JSONObject punishdata = item.getJSONObject("punishment");
			int code = punishdata.getInt("code");
			long time = punishdata.getLong("time");
			
			Punishment punishment = new Punishment(ip, code, time);
			blacklist.put(ip, punishment);
		}
	}
	
	private static void parseWhitelist(JSONArray list) {
		for (int i = 0; i < list.length(); i++) {
			String ip = list.getString(i);
			whitelist.add(ip);
		}
	}
	
	public static boolean clear(String ip) {
		if (whitelist.contains(ip)) {
			return true;
		}
		if (blacklist.containsKey(ip)) {
			Punishment punishment = blacklist.get(ip);
			if (punishment.getTime() < new Date().getTime()) {
				blacklist.remove(ip);
				return true;
			}
			if ((punishment.getCode() & 0x01) > 0) {
				return false;
			}
		}
		return true;
	}
}
