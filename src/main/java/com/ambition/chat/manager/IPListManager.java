package com.ambition.chat.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
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
	private static final List<String> whitelist;
	
	static {
		logger = LogManager.getLogger(IPListManager.class);
		IPLIST_REQ_URL = "http://localhost:81/websocket/data/iplist.json";
		blacklist = new HashMap<>();
		whitelist = new ArrayList<>();
	}
	
	public static void load() {
		CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
		HttpGet request = new HttpGet(IPLIST_REQ_URL);
		
		Future<HttpResponse> future = null;
		HttpResponse response = null;
		try {
			client.start();
			future = client.execute(request, null);
			response = future.get();
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
