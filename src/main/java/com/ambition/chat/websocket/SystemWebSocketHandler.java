package com.ambition.chat.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SystemWebSocketHandler implements WebSocketHandler {

	private static final Log logger;
	private static final Map<String, List<WebSocketSession>> groups;
	
    static {
    	logger = LogFactory.getLog(SystemWebSocketHandler.class);
        groups = new HashMap<>();
    }
	
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// TODO Auto-generated method stub
		Object userId = session.getAttributes().get(Constants.SESSION_USERID);
		Object nickname = session.getAttributes().get(Constants.SESSION_NICKNAME);
		Object usertype = session.getAttributes().get(Constants.SESSION_USERTYPE);
		Object channelId = session.getAttributes().get(Constants.SESSION_CHANNELID);
		if (userId == null) {
			logger.info("User id not found while connection established.");
			return;
		}
		
		List<WebSocketSession> users;
		if (groups.containsKey(channelId.toString())) {
			users = groups.get(channelId.toString());
		} else {
			users = new ArrayList<>();
			groups.put(channelId.toString(), users);
		}
		users.add(session);
		
		session.sendMessage(new TextMessage("{"
				+ "\"raw\": \"ident\","
				+ "\"user\": {"
					+ "\"id\": " + userId + ","
					+ "\"name\": \"" + nickname + "\","
					+ "\"role\": " + usertype + ","
					+ "\"interval\": " + getMessageInterval((int) usertype)
				+ "},"
				+ "\"channel\": {"
					+ "\"id\": " + channelId
				+ "}"
			+ "}"));
		
		if ((int) usertype > 0) {
			TextMessage message = new TextMessage("{"
					+ "\"raw\": \"join\","
					+ "\"user\": {"
						+ "\"id\": " + userId + ","
						+ "\"name\": \"" + nickname + "\","
						+ "\"role\": " + usertype
					+ "},"
					+ "\"channel\": {"
						+ "\"id\": " + channelId
					+ "}"
				+ "}");
			send2all(users, message);
		}
        logger.info(userId + " connected.");
	}

	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		// TODO Auto-generated method stub
		String str = message.getPayload().toString();
		if (str.equals("java.nio.HeapByteBuffer[pos=0 lim=0 cap=0]")) {
			return;
		}
		logger.info("Received: " + message.getPayload().toString());
		
		Object userId = session.getAttributes().get(Constants.SESSION_USERID);
		Object nickname = session.getAttributes().get(Constants.SESSION_NICKNAME);
		Object usertype = session.getAttributes().get(Constants.SESSION_USERTYPE);
		Object channelId = session.getAttributes().get(Constants.SESSION_CHANNELID);
		Object lastsent = session.getAttributes().get(Constants.SESSION_LASTSENT);
		if (userId == null) {
			logger.info("User id not found while handling message.");
			return;
		}
		TextMessage msg;
		Date now = new Date();
		int currentTime = ((Number) now.getTime()).intValue();
		if (lastsent != null) {
			int interval = getMessageInterval((int) usertype);
			if (interval < 0 || currentTime - (int) lastsent < interval) {
				logger.info("Frequency limited while handling message from " + userId + "(" + nickname + ")" + ".");
				msg = new TextMessage("{"
						+ "\"raw\": \"error\","
						+ "\"user\": {"
							+ "\"id\": " + userId + ","
							+ "\"name\": \"" + nickname + "\","
							+ "\"role\": " + usertype
						+ "},"
						+ "\"data\": {"
							+ "\"code\": 409,"
							+ "\"explain\": \"Conflict\","
							+ "\"type\": \"multi\","
							+ "\"text\": \"" + message.getPayload() + "\""
						+ "},"
						+ "\"channel\": {"
							+ "\"id\": " + channelId
						+ "}"
					+ "}");
				session.sendMessage(msg);
				return;
			}
		}
		session.getAttributes().put(Constants.SESSION_LASTSENT, currentTime);
		
		List<WebSocketSession> users = groups.get(channelId.toString());
		msg = new TextMessage("{"
				+ "\"raw\": \"message\","
				+ "\"user\": {"
					+ "\"id\": " + userId + ","
					+ "\"name\": \"" + nickname + "\","
					+ "\"role\": " + usertype 
				+ "},"
				+ "\"data\": {"
					+ "\"type\": \"multi\","
					+ "\"text\": \"" + message.getPayload() + "\""
				+ "},"
				+ "\"channel\": {"
					+ "\"id\": " + channelId
				+ "}"
			+ "}");
		send2all(users, msg);
	}
	
	private int getMessageInterval(long usertype) {
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

	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// TODO Auto-generated method stub
		try {
			if (session.isOpen()) {
				session.close();
			}
			remove(session);
			logger.info("Websocket connection closed with transport error.");
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		// TODO Auto-generated method stub
		Object userId = session.getAttributes().get(Constants.SESSION_USERID);
		Object nickname = session.getAttributes().get(Constants.SESSION_NICKNAME);
		Object usertype = session.getAttributes().get(Constants.SESSION_USERTYPE);
		Object channelId = session.getAttributes().get(Constants.SESSION_CHANNELID);
		if (userId == null) {
			logger.info("User id not found while connection closed.");
			return;
		}
		if ((int) usertype > 0) {
			List<WebSocketSession> users = groups.get(channelId.toString());
			TextMessage message = new TextMessage("{"
					+ "\"raw\": \"left\","
					+ "\"user\": {"
						+ "\"id\": " + userId + ","
						+ "\"name\": \"" + nickname + "\","
						+ "\"role\": " + usertype
					+ "},"
					+ "\"channel\": {"
						+ "\"id\": " + channelId
					+ "}"
				+ "}");
			send2all(users, message);
        }
		
		remove(session);
		logger.info("Websocket connection closed with " + status.getCode());
	}
	
	public boolean supportsPartialMessages() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void send2all(List<WebSocketSession> users, TextMessage message) {
		for (WebSocketSession user : users) {
			try {
				if (user.isOpen()) {
					user.sendMessage(message);
				}
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}
	
	private void remove(WebSocketSession session) {
		Object channelId = session.getAttributes().get(Constants.SESSION_CHANNELID);
		List<WebSocketSession> users = groups.get(channelId.toString());
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
	
}
