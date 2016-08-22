package com.ambition.chat.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

public class PunishmentManager {

	private static final Logger logger;
	private static final String PUNISHMENT_REQ_URL;
	private static final Map<String, Map<String, Punishment>> punishments;
	
	private static final HttpLoader loader;
	
	static {
		logger = LogManager.getLogger(PunishmentManager.class);
		PUNISHMENT_REQ_URL = "http://localhost/websocket/data/punished.json";
		punishments = new HashMap<>();
		
		loader = new HttpLoader();
		loader.addEventListener(Event.COMPLETE, new EventListener() {
			
			@Override
			public void callback(Event e) {
				LoaderEvent evt = (LoaderEvent) e;
				JSONObject list = Utils.parse(evt.response());
				if (list == null) {
					return;
				}
				parseIPList(list);
				
				logger.warn("Getting punishing list completed.");
			}
		});
		loader.addEventListener(Event.ERROR, new EventListener() {
			
			@Override
			public void callback(Event e) {
				ErrorEvent evt = (ErrorEvent) e;
				logger.error("Failed to load punishing list. Explain: " + evt.explain());
			}
		});
		loader.addEventListener(LoaderEvent.CANCEL, new EventListener() {
			
			@Override
			public void callback(Event e) {
				logger.error("Loading punishing list cancelled.");
			}
		});
	}
	
	public static void load() {
		loader.load(PUNISHMENT_REQ_URL, null, null);
	}
	
	public static void syncLoad() {
		loader.syncLoad(PUNISHMENT_REQ_URL, null, null);
	}
	
	private static void parseIPList(JSONObject list) {
		Iterator<String> it = list.keys();
		while (it.hasNext()) {
			String channelId = it.next();
			
			Map<String, Punishment> channel;
			if (punishments.containsKey(channelId) == false) {
				channel = new HashMap<>();
				punishments.put(channelId, channel);
			} else {
				channel = punishments.get(channelId);
			}
			
			JSONArray items = list.getJSONArray(channelId);
			for (int i = 0; i < items.length(); i++) {
				JSONObject item = items.getJSONObject(i);
				if (item.has("id") == false || item.has("punishment") == false) {
					continue;
				}
				
				int userId = item.getInt("id");
				
				JSONObject punishdata = item.getJSONObject("punishment");
				int code = punishdata.getInt("code");
				long time = punishdata.getLong("time");
				
				Punishment punishment = new Punishment(channelId, userId, code, time);
				channel.put(String.valueOf(userId), punishment);
			}
		}
	}
	
	public static void add(String channelId, int userId, Punishment punishment) {
		Map<String, Punishment> channel;
		if (punishments.containsKey(channelId) == false) {
			channel = new HashMap<>();
			punishments.put(channelId, channel);
		} else {
			channel = punishments.get(channelId);
		}
		channel.put(String.valueOf(userId), punishment);
	}
	
	public static Punishment get(String channelId, int userId) {
		Map<String, Punishment> channel = punishments.get(channelId);
		if (channel == null) {
			return null;
		}
		Punishment punishment = channel.get(String.valueOf(userId));
		if (punishment != null && punishment.getTime() <= new Date().getTime()) {
			channel.remove(String.valueOf(userId));
			return null;
		}
		return punishment;
	}
	
	public static Punishment remove(String channelId, int userId) {
		Map<String, Punishment> channel = punishments.get(channelId);
		if (channel == null) {
			return null;
		}
		return channel.remove(String.valueOf(userId));
	}
	
	public static int check(String channelId, int userId, int code) {
		Punishment punishment = get(channelId, userId);
		if (punishment == null) {
			return 0;
		}
		return punishment.getCode() & code;
	}
}
