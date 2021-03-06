package com.ambition.chat.websocket;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.ambition.chat.manager.IPListManager;
import com.ambition.chat.utils.Utils;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

	private static final Logger logger;
	
	static {
		logger = LogManager.getLogger(WebSocketHandshakeInterceptor.class);
	}
	
	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String, Object> attributes) throws Exception {
		// TODO Auto-generated method stub
		String ip = Utils.getIpAddress(request);
		logger.warn("Connecting from " + ip);
		if (IPListManager.clear(ip) == false) {
			logger.warn("Shutdown connection from " + ip);
			return false;
		}
		
		if (request.getHeaders().containsKey("Sec-WebSocket-Extensions")) {
			request.getHeaders().set("Sec-WebSocket-Extensions", "permessage-deflate");
		}
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
			
			@SuppressWarnings("unused")
			HttpSession session = servletRequest.getServletRequest().getSession(false);
			//logger.warn("Sessions " + (session == null ? "not " : "") + "found before handshake.");
		}
		return true;
	}
	
	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {
		// TODO Auto-generated method stub
		
	}
	
}
