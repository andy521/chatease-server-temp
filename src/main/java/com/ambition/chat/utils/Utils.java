package com.ambition.chat.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

public class Utils {

	private static final Logger logger;
	private static final Pattern pattern;
	private static Matcher matcher;
	
	static {
		logger = LogManager.getLogger(Utils.class);
		pattern = Pattern.compile("(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}");
	}
	
	public static JSONObject parse(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			return null;
		}
		
		JSONObject data = null;
		try {
			InputStream instream = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				int endIndex = line.indexOf("//");
				builder.append(line.substring(0, endIndex == -1 ? line.length() : endIndex) + "\n");
			}
			instream.close();
			data  = new JSONObject(builder.toString());
		} catch (Exception e) {
			logger.error("Failed to read response data. Error: " + e.toString());
		}
		
		return data;
	}
	
	public static JSONObject random(String channelId) {
		int randomId = (int) Math.floor(Math.random() * 100) * -1;
		
		JSONObject userdata = new JSONObject();
		userdata.put("id", randomId);
		userdata.put("name", "�ο�" + Math.abs(randomId));
		
		JSONObject channeldata = new JSONObject();
		channeldata.put("id", channelId);
		channeldata.put("role", -1);
		channeldata.put("state", 3);
		
		JSONObject logindata = new JSONObject();
		logindata.put("user", userdata);
		logindata.put("channel", channeldata);
		
		return logindata;
	}
	
	public static int getIntervalByRole(int role) {
		int val = -1;
		if (role < 0) {
			val = 3000;
		} else if (role == 0) {
			val = 2000;
		} else if (role >= 8) {
			val = 0;
		} else if ((role & 0x07) > 0) {
			val = 1000;
		}
		return val;
	}
	
	public static String getIpAddress(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		String ip = null;
		
		List<String> list = headers.get("X-Real-IP");
		if (list == null) {
			list = headers.get("x-forwarded-for");
		}
		if (list == null) {
			list = headers.get("Proxy-Client-IP");
		}
		if (list == null) {
			list = headers.get("WL-Proxy-Client-IP");
		}
		if (list != null && list.size() > 0) {
			ip = list.get(0);
		}
		if(ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
			ip = request.getRemoteAddress().getAddress().toString();
		}
		matcher = pattern.matcher(ip);
		if (matcher.find()) {
			ip = matcher.group();
		}
		return ip;
	}
	
	public static String addQuery(String url, String q) {
		if (q == null || q.length() == 0) {
			return url;
		}
		return url + (url.indexOf("?") == -1 ? "?" : "&") + q;
	}
}
