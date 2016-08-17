package com.ambition.chat.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

public class HttpUserInfoLoader {

	private static final Logger logger;
	private static final String USERINFO_REQ_URL;
	
	static {
		logger = LogManager.getLogger(HttpUserInfoLoader.class);
		USERINFO_REQ_URL = "http://localhost/websocket/data/userinfo.json";
		//USERINFO_REQ_URL = "http://192.168.1.227:8080/live/method=httpChatRoom";
	}
	
	public static JSONObject load(String channelId, String tokenId) {
		HttpClient client = new DefaultHttpClient();
		
		HttpGet request = new HttpGet(USERINFO_REQ_URL + "?channel=" + channelId + "&token=" + tokenId);
		
		/*JSONObject param = new JSONObject();
		param.put("channelId", channelId);
		param.put("uniqueKey", token);
		
		HttpPost request = new HttpPost(USERINFO_REQ_URL);
		request.addHeader("Content-type", "application/json; charset=utf-8");
		request.setHeader("Accept", "application/json");
		request.setEntity(new StringEntity(param.toString(), Charset.forName("UTF-8")));*/
		
		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (Exception e) {
			logger.error("Failed to get user info. Error: " + e.toString());
			return null;
		}
		logger.warn("Got user info response: " + response.getStatusLine());
		
		JSONObject info = null;
		int statuscode = response.getStatusLine().getStatusCode();
		switch (statuscode) {
			case 200:
				info = Utils.parse(response);
				break;
			case 301:
			case 302:
				/*if (response.containsHeader("location")) {
					USERINFO_REQ_URL = response.getHeaders("location").toString();
					load(channelId, tokenId);
				}*/
				break;
			default:
				
				break;
		}
		//httpclient.getConnectionManager().shutdown();
		
		return info;
	}
}