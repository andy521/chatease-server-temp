package com.ambition.chat.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.ambition.chat.common.Channel;
import com.ambition.chat.common.Constants;
import com.ambition.chat.common.User;
import com.ambition.chat.utils.HttpUserInfoLoader;

@Component
public class SystemWebSocketHandler implements WebSocketHandler {

	private static final Logger logger;
	private static final Map<String, List<WebSocketSession>> groups;
	
	static {
		logger = LogManager.getLogger(SystemWebSocketHandler.class);
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
		if (data == null || data.has("cmd") == false 
				|| data.has("channel") == false || data.getJSONObject("channel").has("id") == false) {
			sendError(session, 400);
			logger.warn("Bad message format.");
			return;
		}
		
		String channelId = data.getJSONObject("channel").get("id").toString();
		User user = this.getAttributes(session);
		if (user.get(channelId).setActive() == false) {
			sendError(session, 409, data);
			logger.warn("Frequency limited while handling cmd=" + data.getString("cmd") + ".");
			return;
		}
		
		String cmd = data.getString("cmd");
		switch (cmd) {
		case "join":
			onJoin(session, data);
			break;
		case "message":
			if (data.has("text") == false || data.has("type") == false) {
				sendError(session, 400);
				logger.warn("Bad request while handling cmd=" + cmd + ".");
				break;
			}
			onMessage(session, data);
			break;
		default:
			sendError(session, 404, data);
			logger.warn("Command=" + cmd + " not found.");
		}
	}
	
	private void onJoin(WebSocketSession session, JSONObject data) throws Exception {
		// TODO Auto-generated method stub
		String channelId = data.getJSONObject("channel").get("id").toString();
		String tokenId = data.has("token") ? data.get("token").toString() : "";
		JSONObject logindata = HttpUserInfoLoader.load(channelId, tokenId);
		if (logindata == null) {
			logindata = new JSONObject();
			logger.error("Unable to get user info, fall through.");
		}
		if (logindata.has("user") == false) {
			int randomId = (int) Math.floor(Math.random() * 100) * -1;
			
			JSONObject userjson = new JSONObject();
			userjson.put("id", randomId);
			userjson.put("name", "сн©м" + Math.abs(randomId));
			
			logindata.put("user", userjson);
		}
		if (logindata.has("channel") == false) {
			JSONObject channeljson = new JSONObject();
			channeljson.put("id", channelId);
			channeljson.put("role", -1);
			channeljson.put("state", 3);
			
			logindata.put("channel", channeljson);
		}
		
		// Add user into channel.
		JSONObject userinfo = logindata.getJSONObject("user");
		int userId = userinfo.getInt("id");
		String nickname = userinfo.getString("name");
		
		JSONObject channelinfo = logindata.getJSONObject("channel");
		int role = channelinfo.getInt("role");
		int state = channelinfo.getInt("state");
		
		if (state <= 0) {
			sendError(session, 403, data);
			logger.warn("Permission denied while joining channel " + channelId);
			return;
		}
		
		User user = this.getAttributes(session);
		user.setProperties(userId, nickname);
		Channel channel = user.get(channelId);
		channel.setProperties(role, state);
		
		add(session, channelId);
		
		// Return identity data.
		JSONObject userjson = user.getJson();
		JSONObject channeljson = channel.getJson();
		JSONObject identdata = new JSONObject();
		identdata.put("raw", "identity");
		identdata.put("user", userjson);
		identdata.put("channel", channeljson);
		
		session.sendMessage(new TextMessage(identdata.toString()));
		
		// Broadcast joining message in the channel.
		if (channel.getRole() > 0) {
			JSONObject joindata = new JSONObject();
			joindata.put("raw", "join");
			joindata.put("user", userjson);
			joindata.put("channel", channeljson);
			
			send2all(new TextMessage(joindata.toString()), channelId);
		}
        logger.info("User " + userjson.toString() + " joined in channel " + channeljson.toString());
	}
	
	private void onMessage(WebSocketSession session, JSONObject data) throws Exception {
		// TODO Auto-generated method stub
		User user = this.getAttributes(session);
		if (user.initialized() == false) {
			sendError(session, 401);
			if (session.isOpen()) {
				session.close();
			}
			logger.error("User ID not found while handling message: " + data.getString("text"));
			return;
		}
		
		String channelId = data.getJSONObject("channel").get("id").toString();
		if (user.joined(channelId) == false) {
			sendError(session, 406);
			logger.error("Channel=" + channelId + " not joined while handling message: " + data.getString("text"));
			return;
		}
		
		String text = data.getString("text");
		String msgtype = data.getString("type");
		
		JSONObject userjson = user.getJson();
		JSONObject channeljson = user.get(channelId).getJson();
		
		JSONObject msgdata = new JSONObject();
		msgdata.put("raw", "message");
		msgdata.put("text", text);
		msgdata.put("type", msgtype);
		msgdata.put("user", userjson);
		msgdata.put("channel", channeljson);
		
		TextMessage message = new TextMessage(msgdata.toString());
		int uniId = 0;
		if (msgtype.equals("uni")) {
			uniId = data.getJSONObject("user").getInt("id");
			send2user(message, channelId, uniId);
		} else {
			send2all(message, channelId);
		}
		logger.info("User " + userjson.toString() + " sent message=" + text + " "
				+ "to " + (msgtype.equals("uni") ? "user " + uniId : "channel " + channelId) + ".");
	}
	
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// TODO Auto-generated method stub
		User user = this.getAttributes(session);
		if (user.initialized() == true) {
			Map<String, Channel> channels = user.getChannels();
			String channelIds = "";
			for (String channelId : channels.keySet()) {
				channelIds += (channelId.length() > 0 ? ", " : "") + channelId;
			}
			logger.warn("User " + user.getJson().toString() + " "
					+ " in channel[s] " + channelIds + " transport error: \r\t" + exception.toString());
		}
		
		if (session.isOpen()) {
			session.close();
		}
	}
	
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// TODO Auto-generated method stub
		User user = this.getAttributes(session);
		if (user.initialized() == false) {
			logger.warn("User id not found while connection closed.");
			return;
		}
		
		JSONObject userjson = user.getJson();
		
		Map<String, Channel> channels = user.getChannels();
		for (String channelId : channels.keySet()) {
			Channel channel = channels.get(channelId);
			if (channel.getRole() > 0) {
				JSONObject channeljson = channel.getJson();
				
				JSONObject leftdata = new JSONObject();
				leftdata.put("raw", "left");
				leftdata.put("user", userjson);
				leftdata.put("channel", channeljson);
				
				send2all(new TextMessage(leftdata.toString()), channelId);
				
				remove(session, channelId);
				logger.info("User " + userjson.toString() + " left channel " + channeljson.toString() 
						+ " with " + status.getCode());
	        }
		}
	}
	
	
	private void send2all(TextMessage message, String channelId) throws Exception {
		// TODO Auto-generated method stub
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			return;
		}
		for (WebSocketSession session : sessions) {
			if (session.isOpen() == false) {
				continue;
			}
			session.sendMessage(message);
		}
	}
	
	private void send2user(TextMessage message, String channelId, int userId) throws Exception {
		// TODO Auto-generated method stub
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			return;
		}
		for (WebSocketSession session : sessions) {
			User user = this.getAttributes(session);
			if (user.initialized() == false) {
				continue;
			}
			if (user.getId() == userId) {
				session.sendMessage(message);
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
		case 406:
			explain = "Not Acceptable";
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
	
	private User getAttributes(WebSocketSession session) {
		Map<String, Object> attributes = session.getAttributes();
		User user;
		if (attributes.containsKey(Constants.SESSION_USER) == false) {
			user = new User();
			attributes.put(Constants.SESSION_USER, user);
		} else {
			user = (User) attributes.get(Constants.SESSION_USER);
		}
		return user;
	}
	
	private void add(WebSocketSession session, String channelId) {
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			sessions = new ArrayList<>();
			groups.put(channelId, sessions);
		}
		sessions.add(session);
	}
	
	private void remove(WebSocketSession session, String channelId) {
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			return;
		}
		for (Iterator<WebSocketSession> it = sessions.iterator(); it.hasNext();) {
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
}
