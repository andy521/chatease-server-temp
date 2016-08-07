package com.ambition.chat.websocket;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

	private static final Log logger = LogFactory.getLog(WebSocketHandshakeInterceptor.class);
	private static final String USERINFO_REQ_URL = "http://localhost:8080/websocket/userinfo/userinfo.json";
	//private static final String USERINFO_REQ_URL = "http://192.168.1.227:8080/live/method=httpChatRoom";
	
	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String, Object> attributes) throws Exception {
		// TODO Auto-generated method stub
		if (request.getHeaders().containsKey("Sec-WebSocket-Extensions")) {
			request.getHeaders().set("Sec-WebSocket-Extensions", "permessage-deflate");
		}
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
			HttpSession session = servletRequest.getServletRequest().getSession(false);
			if (session == null) {
				Map<String, String[]> params = servletRequest.getServletRequest().getParameterMap();
				JSONObject userinfo = null;
				if (params != null && params.containsKey("token")) {
					userinfo = getUserInfo(params.get("token")[0]);
				}
				if (userinfo == null) {
					userinfo = new JSONObject();
					userinfo.put("status", false);
				}
				if ((Boolean) userinfo.get("status") == false) {
					JSONObject info = new JSONObject();
					int userId = (int) Math.floor(Math.random() * 100) * -1;
					info.put("id", userId);
					info.put("name", "сн©м" + Math.abs(userId));
					info.put("type", -1);
					userinfo.put("info", info);
					
					JSONObject channel = new JSONObject();
					channel.put("id", 3);
					userinfo.put("channel", channel);
				}
				attributes.put(Constants.SESSION_USERID, ((JSONObject) userinfo.get("info")).get("id"));
				attributes.put(Constants.SESSION_NICKNAME, ((JSONObject) userinfo.get("info")).get("name"));
				attributes.put(Constants.SESSION_USERTYPE, ((JSONObject) userinfo.get("info")).get("type"));
				attributes.put(Constants.SESSION_CHANNELID, ((JSONObject) userinfo.get("channel")).get("id"));
			}
		}
		return true;
	}
	
	private JSONObject getUserInfo(String token) {
		HttpClient httpclient = new DefaultHttpClient();
		
		JSONObject param = new JSONObject();
		param.put("uniqueKey", token);
		
		HttpGet request = new HttpGet(USERINFO_REQ_URL + "?token=" + token);
		
		/*HttpPost request = new HttpPost(USERINFO_REQ_URL);
		request.addHeader("Content-type", "application/json; charset=utf-8");
		request.setHeader("Accept", "application/json");
		request.setEntity(new StringEntity(param.toString(), Charset.forName("UTF-8")));*/
		
		HttpResponse httpresponse = null;
		try {
			httpresponse = httpclient.execute(request);
		} catch (Exception e) {
			//e.printStackTrace();
			logger.info("Failed to get user info. Error: " + e.toString());
			return null;
		}
		logger.info("Response status: " + httpresponse.getStatusLine());
		
		JSONObject info = null;
		int statuscode = httpresponse.getStatusLine().getStatusCode();
		switch (statuscode) {
			case 200:
				info = readResponse(httpresponse);
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
	
	private JSONObject readResponse(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			return null;
		}
		
		JSONObject data = null;
		try {
			//logger.info("Entity: " + EntityUtils.toString(entity));
			
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
			e.printStackTrace();
		}
		
		return data;
	}
	
	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {
		// TODO Auto-generated method stub
		
	}
	
}
