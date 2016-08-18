package com.ambition.chat.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

public class PunishmentManager {

	private static final Logger logger;
	private static final String PUNISHMENT_REQ_URL;
	private static final Map<String, Map<String, Punishment>> punishments;
	
	static {
		logger = LogManager.getLogger(IPListManager.class);
		PUNISHMENT_REQ_URL = "http://localhost:81/websocket/data/punished.json";
		punishments = new HashMap<>();
	}
	
	public static void load() {
		CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
		HttpGet request = new HttpGet(PUNISHMENT_REQ_URL);
		
		Future<HttpResponse> future = null;
		HttpResponse response = null;
		try {
			client.start();
			future = client.execute(request, null);
			response = future.get();
		} catch (Exception e) {
			logger.error("Failed to get punishment list. Error: " + e.toString());
			return;
		}
		logger.warn("Got response of punishment list: " + response.getStatusLine());
		
		JSONObject list = null;
		int statuscode = response.getStatusLine().getStatusCode();
		switch (statuscode) {
			case 200:
				list = Utils.parse(response);
				break;
			case 301:
			case 302:
				/*if (response.containsHeader("location")) {
					PUNISHMENT_REQ_URL = response.getHeaders("location").toString();
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
		parseList(list);
		logger.warn("Got punishment list.");
	}
	
	private static void parseList(JSONObject list) {
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
