package com.ambition.chat.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

public class Utils {

	private static final Logger logger;
	
	static {
		logger = LogManager.getLogger(Utils.class);
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
		return ip;
	}
}
