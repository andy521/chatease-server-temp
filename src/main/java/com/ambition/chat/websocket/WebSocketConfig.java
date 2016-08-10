package com.ambition.chat.websocket;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.ambition.chat.websocket.SystemWebSocketHandler;
import com.ambition.chat.websocket.WebSocketHandshakeInterceptor;

@Configuration
@EnableWebMvc
@EnableWebSocket
public class WebSocketConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {

	private static final Logger logger;
	
	static {
		logger = LogManager.getLogger(WebSocketHandshakeInterceptor.class);
	}
	
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// TODO Auto-generated method stub
		registry.addHandler(systemWebSocketHandler(), "/websck").addInterceptors(new WebSocketHandshakeInterceptor());
		registry.addHandler(systemWebSocketHandler(), "/websck/sockjs").addInterceptors(new WebSocketHandshakeInterceptor()).withSockJS();
		logger.info("=== Websocket handler registed ===");
	}
	
	public WebSocketHandler systemWebSocketHandler() {
        return new SystemWebSocketHandler();
    }
}
