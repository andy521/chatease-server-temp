package com.ambition.chat.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SystemWebSocketHandler implements WebSocketHandler {

	private static final Logger logger;
	private static final String USERINFO_REQ_URL;
	private static final Map<String, List<WebSocketSession>> groups;
	
    static {
    	logger = LogManager.getLogger(SystemWebSocketHandler.class);
    	USERINFO_REQ_URL = "http://localhost/websocket/userinfo/userinfo.json";
		//USERINFO_REQ_URL = "http://192.168.1.227:8080/live/method=httpChatRoom";
        groups = new HashMap<>();
    }
	
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		// TODO Auto-generated method stub
		String msgstr = message.getPayload().toString();
		if (msgstr.equals("java.nio.HeapByteBuffer[pos=0 lim=0 cap=0]")) {
			// Ignore IE issue.
			return;
		}
		
		JSONObject data = null;
		try {
			data = new JSONObject(msgstr);
		} catch (Exception e) {
			logger.error("Failed to parse message: " + msgstr + ". Error: \r\t" + e.toString());
		}
		if (data == null || data.has("cmd") == false) {
			sendError(session, 400);
			return;
		}
		
		String cmd = (String) data.get("cmd");
		switch (cmd) {
		case "join":
			JSONObject channel = (JSONObject) data.get("channel");
			if (channel == null || channel.has("id") == false) {
				break;
			}
			onJoin(session, data);
			break;
		case "message":
			JSONObject pipe = (JSONObject) data.get("pipe");
			if (data.has("text") == false 
					|| pipe == null || pipe.has("type") == false || pipe.has("id") == false) {
				break;
			}
			onMessage(session, data);
			break;
		default:
			sendError(session, 404);
		}
	}
	
	private void onJoin(WebSocketSession session, JSONObject data) throws Exception {
		// TODO Auto-generated method stub
		String channelId = ((JSONObject) data.get("channel")).get("id").toString();
		String tokenId = data.has("token") ? data.get("token").toString() : "";
		JSONObject userinfo = HttpUserInfoLoader.load(channelId, tokenId);
		if (userinfo == null) {
			userinfo = new JSONObject();
			logger.warn("Unable to get user info, fall through.");
		} else if (!userinfo.has("status") || (Boolean) userinfo.get("status") == false) {
			sendError(session, 403, new JSONObject()
				.put("method", data.get("cmd"))
				.put("channel", data.get("channel"))
			);
			logger.warn("Permission denied while joining channel " + channelId);
			return;
		}
		if (userinfo.has("info") == false) {
			int userId = (int) Math.floor(Math.random() * 100) * -1;
			
			JSONObject info = new JSONObject();
			info.put("id", userId);
			info.put("name", "сн©м" + Math.abs(userId));
			info.put("type", -1);
			
			userinfo.put("info", info);
		}
		if (userinfo.has("channel") == false) {
			JSONObject channel = new JSONObject();
			channel.put("id", channelId);
			
			userinfo.put("channel", channel);
		}
		
		// Add user into channel.
		int userId = (int) ((JSONObject) userinfo.get("info")).get("id");
		String nickname = (String) ((JSONObject) userinfo.get("info")).get("name");
		int usertype = (int) ((JSONObject) userinfo.get("info")).get("type");
		
		Map<String, Object> attributes = session.getAttributes();
		attributes.put(Constants.SESSION_USERID, userId);
		attributes.put(Constants.SESSION_NICKNAME, nickname);
		attributes.put(Constants.SESSION_USERTYPE, usertype);
		attributes.put(Constants.SESSION_CHANNELID, channelId);
		
		add(session, channelId);
		
		// Return ident data.
		JSONObject user = new JSONObject();
		user.put("id", userId);
		user.put("name", nickname);
		user.put("role", usertype);
		user.put("interval", getSendingInterval((int) usertype));
		
		JSONObject channel = new JSONObject();
		channel.put("id", channelId);
		
		JSONObject identdata = new JSONObject();
		identdata.put("raw", "ident");
		identdata.put("user", user);
		identdata.put("channel", channel);
		
		session.sendMessage(new TextMessage(identdata.toString()));
		
		// Broadcast joining message in the channel.
		if ((int) usertype > 0) {
			user.remove("interval");
			
			JSONObject joindata = new JSONObject();
			joindata.put("raw", "join");
			joindata.put("user", user);
			joindata.put("channel", channel);
			
			send2all(new TextMessage(joindata.toString()), channelId);
		}
        logger.info("User { id: " + userId + ", name: " + nickname + " } joined in channel " + channelId);
	}
	
	private void onMessage(WebSocketSession session, JSONObject data) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> attributes = session.getAttributes();
		if (attributes.containsKey(Constants.SESSION_USERID) == false) {
			logger.error("User ID not found while handling message: " + data.getString("text"));
			return;
		}
		
		int userId = (int) attributes.get(Constants.SESSION_USERID);
		String nickname = (String) attributes.get(Constants.SESSION_NICKNAME);
		int usertype = (int) attributes.get(Constants.SESSION_USERTYPE);
		
		JSONObject pipe = (JSONObject) data.get("pipe");
		String pipetype = (String) pipe.get("type");
		String pipeId = pipe.get("id").toString();
		
		String text = data.getString("text");
		
		int lastsent = 0;
		if (attributes.containsKey(Constants.SESSION_LASTSENT)) {
			lastsent = (int) attributes.get(Constants.SESSION_LASTSENT);
		}
		
		JSONObject user = new JSONObject();
		user.put("id", userId);
		user.put("name", nickname);
		user.put("role", usertype);
		
		Date now = new Date();
		int currentTime = ((Number) now.getTime()).intValue();
		if (lastsent > 0) {
			int interval = getSendingInterval((int) usertype);
			if (interval < 0 || currentTime - (int) lastsent < interval) {
				sendError(session, 409, new JSONObject()
					.put("text", text)
					.put("pipe", pipe)
				);
				logger.warn("Frequency limited while handling message from "
						+ "user { id: " + userId + ", name: " + nickname + " } "
						+ "to pipe { type: " + pipetype + ", id: " + pipeId + " }.");
				return;
			}
		}
		attributes.put(Constants.SESSION_LASTSENT, currentTime);
		
		JSONObject msgdata = new JSONObject();
		msgdata.put("raw", "message");
		msgdata.put("text", text);
		msgdata.put("user", user);
		msgdata.put("pipe", pipe);
		
		TextMessage message = new TextMessage(msgdata.toString());
		if (pipetype.equals("uni")) {
			send2user(message, pipeId, userId);
		} else {
			send2all(message, pipeId);
		}
		logger.info("User { id: " + userId + ", name: " + nickname + " } "
				+ "sent message=" + text + " to " + (pipetype.equals("uni") ? "user" : "channel") + " " + pipeId);
	}
	
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> attributes = session.getAttributes();
		if (attributes.containsKey(Constants.SESSION_USERID) == true) {
			int userId = (int) attributes.get(Constants.SESSION_USERID);
			String nickname = (String) attributes.get(Constants.SESSION_NICKNAME);
			String channelId = (String) attributes.get(Constants.SESSION_CHANNELID);
			logger.warn("User { id: " + userId + ", name: " + nickname + " } "
					+ " in channel " + channelId + " transport error: \r\t" + exception.toString());
		}
		
		if (session.isOpen()) {
			try {
				session.close();
			} catch (Exception e) {
				logger.error("Error while handling transport error. Error: \r\t" + e.toString());
			}
		}
	}
	
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> attributes = session.getAttributes();
		if (attributes.containsKey(Constants.SESSION_USERID) == false) {
			logger.warn("User id not found while connection closed.");
			return;
		}
		int userId = (int) attributes.get(Constants.SESSION_USERID);
		String nickname = (String) attributes.get(Constants.SESSION_NICKNAME);
		int usertype = (int) attributes.get(Constants.SESSION_USERTYPE);
		String channelId = (String) attributes.get(Constants.SESSION_CHANNELID);
		
		if ((int) usertype > 0) {
			JSONObject user = new JSONObject();
			user.put("id", userId);
			user.put("name", nickname);
			user.put("role", usertype);
			
			JSONObject channel = new JSONObject();
			channel.put("id", channelId);
			
			JSONObject leftdata = new JSONObject();
			leftdata.put("raw", "left");
			leftdata.put("user", user);
			leftdata.put("channel", channel);
			
			send2all(new TextMessage(leftdata.toString()), channelId);
        }
		
		remove(session);
		logger.info("User { id: " + userId + ", name: " + nickname + " } "
				+ "left channel " + channelId + " with " + status.getCode());
	}
	
	
	private void send2all(TextMessage message, String channelId) {
		// TODO Auto-generated method stub
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			return;
		}
		for (WebSocketSession session : sessions) {
			if (session.isOpen() == false) {
				continue;
			}
			try {
				session.sendMessage(message);
			} catch (IOException e) {
				Map<String, Object> attributes = session.getAttributes();
				int userId = (int) attributes.get(Constants.SESSION_USERID);
				String nickname = (String) attributes.get(Constants.SESSION_NICKNAME);
				logger.error("Failed to send message to user { id: " + userId + ", name: " + nickname + " } "
						+ "in channel " + channelId + ". Error: \r\t" + e.toString());
			}
		}
	}
	
	private void send2user(TextMessage message, String channelId, int userId) {
		// TODO Auto-generated method stub
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			return;
		}
		for (WebSocketSession session : sessions) {
			Map<String, Object> attributes = session.getAttributes();
			if (attributes.containsKey(Constants.SESSION_USERID) == false) {
				// Should not happen.
				continue;
			}
			int uid = (int) attributes.get(Constants.SESSION_USERID);
			if (uid == userId) {
				try {
					session.sendMessage(message);
				} catch (IOException e) {
					String nickname = (String) attributes.get(Constants.SESSION_NICKNAME);
					logger.error("Failed to send message to user { id: " + userId + ", name: " + nickname + " } "
							+ "in channel " + channelId + ". Error: \r\t" + e.toString());
				}
			}
		}
	}
	
	private void sendError(WebSocketSession session, int code) throws Exception {
		// TODO Auto-generated method stub
		sendError(session, code, null);
	}
	
	private void sendError(WebSocketSession session, int code, JSONObject params) throws Exception {
		// TODO Auto-generated method stub
		String explain = "";
		switch (code) {
		case 400:
			explain = "Bad Request";
			break;
		case 401:
			explain = "Unauthorized";
			break;
		case 403:
			explain = "Forbidden";
			break;
		case 404:
			explain = "Not Found";
			break;
		case 409:
			explain = "Conflict";
			break;
		default:
			logger.error("Unknown error code " + code + ", abort to send error.");
			return;
		}
		
		JSONObject error = new JSONObject();
		error.put("code", code);
		error.put("explain", explain);
		
		JSONObject errordata = new JSONObject();
		errordata.put("raw", "error");
		errordata.put("error", error);
		if (params != null) {
			Iterator<String> it = params.keys();
			while (it.hasNext()) {
				String key = it.next();
				errordata.put(key, params.get(key));
			}
		}
		
		session.sendMessage(new TextMessage(errordata.toString()));
	}
	
	private int getSendingInterval(long usertype) {
		int interval = -1;
		if (usertype < 0) {
			interval = 3000;
		} else if (usertype == 0) {
			interval = 2000;
		} else if (usertype >= 8) {
			interval = 0;
		} else if ((usertype & 0x07) > 0) {
			interval = 1000;
		}
		return interval;
	}
	
	private void add(WebSocketSession session, String channelId) {
		List<WebSocketSession> users = groups.get(channelId);
		if (users == null) {
			users = new ArrayList<>();
			groups.put(channelId, users);
		}
		users.add(session);
	}
	
	private void remove(WebSocketSession session) {
		Map<String, Object> attributes = session.getAttributes();
		if (attributes.containsKey(Constants.SESSION_CHANNELID) == false) {
			return;
		}
		String channelId = (String) attributes.get(Constants.SESSION_CHANNELID);
		List<WebSocketSession> users = groups.get(channelId);
		if (users == null) {
			return;
		}
		for (Iterator<WebSocketSession> it = users.iterator(); it.hasNext();) {
			WebSocketSession user = it.next();
			if (session.getId().equals(user.getId())) {
				it.remove();
			} else if (!user.isOpen()) {
				it.remove();
			}
		}
	}
	
	public boolean supportsPartialMessages() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	static class HttpUserInfoLoader {
		
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
}
