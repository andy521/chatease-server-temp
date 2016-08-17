package com.ambition.chat.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ambition.chat.common.Punishment;
import com.ambition.chat.utils.Utils;

public class IPListManager {

	private static final Logger logger;
	private static final String IPLIST_REQ_URL;
	private static final Map<String, Punishment> blacklist;
	private static final Map<String, String> whitelist;
	
	static {
		logger = LogManager.getLogger(IPListManager.class);
		IPLIST_REQ_URL = "http://localhost/websocket/data/iplist.json";
		blacklist = new HashMap<>();
		whitelist = new HashMap<>();
	}
	
	public static void load() {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(IPLIST_REQ_URL);
		
		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (Exception e) {
			logger.error("Failed to get ip list. Error: " + e.toString());
			return;
		}
		logger.warn("Got response of ip list: " + response.getStatusLine());
		
		JSONObject list = null;
		int statuscode = response.getStatusLine().getStatusCode();
		switch (statuscode) {
			case 200:
				list = Utils.parse(response);
				break;
			case 301:
			case 302:
				/*if (response.containsHeader("location")) {
					IPLIST_REQ_URL = response.getHeaders("location").toString();
					load();
				}*/
				break;
			default:
				
				break;
		}
		//httpclient.getConnectionManager().shutdown();
		
		if (list == null) {
			return;
		}
		if (list.has("blacklist")) {
			parseBlacklist(list.getJSONArray("blacklist"));
		}
		if (list.has("whitelist")) {
			parseWhitelist(list.getJSONArray("whitelist"));
		}
		logger.warn("Got IP list.");
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
			long time = punishdata.getInt("time");
			
			Punishment punishment = new Punishment(ip, code, time);
			blacklist.put("ip", punishment);
		}
	}
	
	private static void parseWhitelist(JSONArray list) {
		for (int i = 0; i < list.length(); i++) {
			String ip = list.getString(i);
			whitelist.put(ip, ip);
		}
	}
	
	public static boolean clear(String ip) {
		if (whitelist.containsKey(ip)) {
			return true;
		}
		if (blacklist.containsKey(ip)) {
			Punishment punishment = blacklist.get(ip);
			if (punishment.getTime() > new Date().getTime()) {
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
