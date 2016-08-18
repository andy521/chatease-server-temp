package com.ambition.chat.common;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class UserInfo {

	private int id = 0;
	private String name = "";
	private boolean initialized = false;
	
	private Map<String, UserInfoInChannel> channels = new HashMap<>();
	
	public UserInfo() {
		// TODO Auto-generated constructor stub
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setProperties(int id, String name) {
		this.id = id;
		this.name = name;
		this.initialized = true;
	}
	
	public boolean initialized() {
		return this.initialized;
	}
	
	public boolean joined(String channelId) {
		if (channels.containsKey(channelId) == false) {
			return false;
		}
		return this.get(channelId).joined();
	}
	
	public JSONObject toJson() {
		JSONObject user = new JSONObject();
		user.put("id", this.id);
		user.put("name", this.name);
		return user;
	}
	
	public UserInfoInChannel get(String channelId) {
		UserInfoInChannel channel;
		if (channels.containsKey(channelId)) {
			channel = channels.get(channelId);
		} else {
			channel = new UserInfoInChannel(channelId);
			channels.put(channelId, channel);
		}
		return channel;
	}
	
	public Map<String, UserInfoInChannel> getChannels() {
		return channels;
	}

}
