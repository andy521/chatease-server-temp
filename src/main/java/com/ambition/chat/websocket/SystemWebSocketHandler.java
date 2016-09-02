package com.ambition.chat.websocket;

import java.io.IOException;
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

import com.ambition.chat.common.Constants;
import com.ambition.chat.common.Punishment;
import com.ambition.chat.common.UserInfo;
import com.ambition.chat.common.UserInfoInChannel;
import com.ambition.chat.events.ErrorEvent;
import com.ambition.chat.events.Event;
import com.ambition.chat.events.EventListener;
import com.ambition.chat.events.LoaderEvent;
import com.ambition.chat.manager.PunishmentManager;
import com.ambition.chat.net.HttpLoader;
import com.ambition.chat.utils.Utils;

@Component
public class SystemWebSocketHandler implements WebSocketHandler {

	private static final Logger logger;
	private static final String USERINFO_REQ_URL;
	private static final Map<String, List<WebSocketSession>> groups;
	
	static {
		logger = LogManager.getLogger(SystemWebSocketHandler.class);
		USERINFO_REQ_URL = "http://localhost/websocket/data/userinfo.json";
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
		if (data == null || data.has("cmd") == false 
				|| data.has("channel") == false || data.getJSONObject("channel").has("id") == false) {
			sendError(session, 400);
			logger.warn("Bad message format.");
			return;
		}
		
		String channelId = data.getJSONObject("channel").get("id").toString();
		UserInfo user = this.getAttributes(session);
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
	
	private void onJoin(final WebSocketSession session, final JSONObject data) {
		// TODO Auto-generated method stub
		final String channelId = data.getJSONObject("channel").get("id").toString();
		final String tokenId = data.has("token") ? data.get("token").toString() : "";
		
		HttpLoader loader = new HttpLoader();
		loader.addEventListener(Event.COMPLETE, new EventListener() {
			
			@Override
			public void callback(Event e) {
				LoaderEvent evt = (LoaderEvent) e;
				logger.warn("Loading user info completed.");
				
				JSONObject logindata = Utils.parse(evt.response());
				if (logindata == null) {
					logindata = Utils.random(channelId);
				}
				onUserInfo(session, data, logindata);
			}
		});
		loader.addEventListener(Event.ERROR, new EventListener() {
			
			@Override
			public void callback(Event e) {
				ErrorEvent evt = (ErrorEvent) e;
				logger.error("Failed to load user info. Explain: " + evt.explain());
				
				JSONObject logindata = Utils.random(channelId);
				if (logindata != null) {
					onUserInfo(session, data, logindata);
				}
			}
		});
		loader.addEventListener(LoaderEvent.CANCEL, new EventListener() {
			
			@Override
			public void callback(Event e) {
				logger.error("Loading user info cancelled.");
				
				JSONObject logindata = Utils.random(channelId);
				if (logindata != null) {
					onUserInfo(session, data, logindata);
				}
			}
		});
		loader.load(USERINFO_REQ_URL, new JSONObject().put("channel", channelId).put("token", tokenId), "GET");
	}
	
	private void onUserInfo(WebSocketSession session, JSONObject data, JSONObject logindata) {
		String channelId = data.getJSONObject("channel").get("id").toString();
		
		if (logindata == null) {
			logindata = new JSONObject();
			logger.error("Unable to get user info, fall through.");
		}
		if (logindata.has("user") == false) {
			int randomId = (int) Math.floor(Math.random() * 100) * -1;
			
			JSONObject userdata = new JSONObject();
			userdata.put("id", randomId);
			userdata.put("name", "сн©м" + Math.abs(randomId));
			
			logindata.put("user", userdata);
		}
		if (logindata.has("channel") == false) {
			JSONObject channeldata = new JSONObject();
			channeldata.put("id", channelId);
			channeldata.put("role", -1);
			channeldata.put("state", 3);
			
			logindata.put("channel", channeldata);
		}
		
		// Add user into channel.
		JSONObject usrinfo = logindata.getJSONObject("user");
		int userId = usrinfo.getInt("id");
		String nickname = usrinfo.getString("name");
		
		JSONObject chaninfo = logindata.getJSONObject("channel");
		int role = chaninfo.getInt("role");
		int state = chaninfo.getInt("state");
		
		if (state <= 0 || PunishmentManager.check(channelId, userId, 0x01) > 0) {
			sendError(session, 403, data);
			logger.warn("Permission denied while joining channel " + channelId);
			return;
		}
		
		UserInfo user = this.getAttributes(session);
		user.setProperties(userId, nickname);
		UserInfoInChannel userinfo = user.get(channelId);
		userinfo.setProperties(role, state);
		
		add(session, channelId);
		
		// Return identity data.
		JSONObject userdata = user.toJson();
		JSONObject channeldata = userinfo.toJson();
		Punishment punishment = PunishmentManager.get(channelId, userId);
		if (punishment != null) {
			channeldata.put("punishment", punishment.toJson());
		}
		
		JSONObject identdata = new JSONObject();
		identdata.put("raw", "identity");
		identdata.put("user", userdata);
		identdata.put("channel", channeldata);
		
		send2session(session, new TextMessage(identdata.toString()));
		
		// Broadcast joining message in the channel.
		if (userinfo.getRole() > 0) {
			JSONObject joindata = new JSONObject();
			joindata.put("raw", "join");
			joindata.put("user", userdata);
			joindata.put("channel", channeldata);
			
			send2all(new TextMessage(joindata.toString()), channelId);
		}
        logger.info("User " + userdata.toString() + " joined in channel " + channeldata.toString());
	}
	
	private void onMessage(WebSocketSession session, JSONObject data) {
		// TODO Auto-generated method stub
		UserInfo user = this.getAttributes(session);
		if (user.initialized() == false) {
			sendError(session, 401);
			if (session.isOpen()) {
				closeSession(session);
			}
			logger.error("User ID not found while handling message: " + data.getString("text"));
			return;
		}
		
		String channelId = data.getJSONObject("channel").get("id").toString();
		if (user.joined(channelId) == false) {
			sendError(session, 406);
			logger.error("User " + user.toJson() + " not joined channel=" + channelId 
					+ " while handling message: " + data.getString("text"));
			return;
		}
		
		if (PunishmentManager.check(channelId, user.getId(), 0x02) > 0) {
			sendError(session, 403, data);
			logger.error("Permission denied while handling message=" + data.getString("text") 
					+ " from User " + user.toJson() + " in channel=" + channelId);
			return;
		}
		
		String text = data.getString("text");
		String msgtype = data.getString("type");
		
		JSONObject userdata = user.toJson();
		JSONObject channeldata = user.get(channelId).toJson();
		
		JSONObject msgdata = new JSONObject();
		msgdata.put("raw", "message");
		msgdata.put("text", text);
		msgdata.put("type", msgtype);
		msgdata.put("user", userdata);
		msgdata.put("channel", channeldata);
		
		TextMessage message = new TextMessage(msgdata.toString());
		int uniId = 0;
		if (msgtype.equals("uni")) {
			uniId = data.getJSONObject("user").getInt("id");
			send2user(message, channelId, uniId);
		} else {
			send2all(message, channelId);
		}
		logger.info(msgdata.toString());
		/*logger.info("User " + userdata.toString() + " sent message=" + text + " "
				+ "to " + (msgtype.equals("uni") ? "user " + uniId : "channel " + channelId) + ".");*/
	}
	
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// TODO Auto-generated method stub
		UserInfo user = this.getAttributes(session);
		if (user.initialized() == true) {
			Map<String, UserInfoInChannel> userinfos = user.getChannels();
			String channelIds = "";
			for (String channelId : userinfos.keySet()) {
				remove(session, channelId);
				channelIds += (channelIds.length() > 0 ? ", " : "") + channelId;
			}
			logger.warn("User " + user.toJson().toString() + " "
					+ " in channel[s] " + channelIds + " transport error: \r\t" + exception.toString());
		}
		closeSession(session);
	}
	
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// TODO Auto-generated method stub
		UserInfo user = this.getAttributes(session);
		if (user.initialized() == false) {
			logger.warn("User id not found while connection closed.");
			return;
		}
		
		JSONObject userdata = user.toJson();
		
		Map<String, UserInfoInChannel> userinfos = user.getChannels();
		for (String channelId : userinfos.keySet()) {
			UserInfoInChannel userinfo = userinfos.get(channelId);
			if (userinfo.getRole() > 0) {
				JSONObject channeldata = userinfo.toJson();
				
				JSONObject leftdata = new JSONObject();
				leftdata.put("raw", "left");
				leftdata.put("user", userdata);
				leftdata.put("channel", channeldata);
				
				send2all(new TextMessage(leftdata.toString()), channelId);
				
				remove(session, channelId);
				logger.info("User " + userdata.toString() + " left channel " + channeldata.toString() 
						+ " with " + status.getCode());
	        }
		}
	}
	
	
	private void send2all(TextMessage message, String channelId) {
		// TODO Auto-generated method stub
		List<WebSocketSession> sessions = groups.get(channelId);
		if (sessions == null) {
			return;
		}
		for (WebSocketSession session : sessions) {
			if (session.isOpen()) {
				send2session(session, message);
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
			UserInfo user = this.getAttributes(session);
			if (user.initialized() == false) {
				continue;
			}
			if (user.getId() == userId) {
				send2session(session, message);
			}
		}
	}
	
	private void sendError(WebSocketSession session, int code) {
		// TODO Auto-generated method stub
		sendError(session, code, null);
	}
	
	private void sendError(WebSocketSession session, int code, JSONObject params) {
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
		send2session(session, new TextMessage(errordata.toString()));
	}
	
	private void send2session(WebSocketSession session, TextMessage message) {
		try {
			session.sendMessage(message);
		} catch (IOException e) {
			// Ignore this.
		}
	}
	
	private void closeSession(WebSocketSession session) {
		if (session.isOpen()) {
			try {
				session.close();
			} catch (IOException e) {
				// Ignore this.
			}
		}
	}
	
	private UserInfo getAttributes(WebSocketSession session) {
		Map<String, Object> attributes = session.getAttributes();
		UserInfo user;
		if (attributes.containsKey(Constants.SESSION_USER) == false) {
			user = new UserInfo();
			attributes.put(Constants.SESSION_USER, user);
		} else {
			user = (UserInfo) attributes.get(Constants.SESSION_USER);
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
