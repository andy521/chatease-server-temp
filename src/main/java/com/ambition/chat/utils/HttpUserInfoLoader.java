package com.ambition.chat.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.ambition.chat.websocket.SystemWebSocketHandler;

public class HttpUserInfoLoader {

	private static final Logger logger;
	private static final String USERINFO_REQ_URL;
	
	static {
		logger = LogManager.getLogger(SystemWebSocketHandler.class);
		USERINFO_REQ_URL = "http://localhost/websocket/userinfo/userinfo.json";
		//USERINFO_REQ_URL = "http://192.168.1.227:8080/live/method=httpChatRoom";
	}
	
	public static JSONObject load(String channelId, String tokenId) {
		HttpClient httpclient = new DefaultHttpClient();
		
		HttpGet request = new HttpGet(USERINFO_REQ_URL + "?channel=" + channelId + "&token=" + tokenId);
		
		/*JSONObject param = new JSONObject();
		param.put("channelId", channelId);
		param.put("uniqueKey", token);
		
		HttpPost request = new HttpPost(USERINFO_REQ_URL);
		request.addHeader("Content-type", "application/json; charset=utf-8");
		request.setHeader("Accept", "application/json");
		request.setEntity(new StringEntity(param.toString(), Charset.forName("UTF-8")));*/
		
		HttpResponse httpresponse = null;
		try {
			httpresponse = httpclient.execute(request);
		} catch (Exception e) {
			logger.error("Failed to get user info. Error: " + e.toString());
			return null;
		}
		logger.warn("Got user info response: " + httpresponse.getStatusLine());
		
		JSONObject info = null;
		int statuscode = httpresponse.getStatusLine().getStatusCode();
		switch (statuscode) {
			case 200:
				info = parse(httpresponse);
				break;
			case 301:
			case 302:
				
				break;
			default:
				
				break;
		}
		//httpclient.getConnectionManager().shutdown();
		
		return info;
	}
	
	private static JSONObject parse(HttpResponse response) {
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

}
