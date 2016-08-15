package com.ambition.chat.websocket;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class UserAttributes {

	private int id = 0;
	private String name = "";
	private Map<String, ChannelAttributes> _channels = new HashMap<>();
	
	public UserAttributes() {
		// TODO Auto-generated constructor stub
	}
	
	public boolean checkInterval(String channelId) {
		// TODO Auto-generated method stub
		int current = ((Number) new Date().getTime()).intValue();
		ChannelAttributes attrs = this.get(channelId);
		int last = attrs.active;
		attrs.active = current;
		if (last > 0) {
			int interval = attrs.getInterval();
			if (interval < 0 || current - last < interval) {
				return false;
			}
		}
		return true;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setInfo(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public boolean logined() {
		return (this.id != 0);
	}
	
	
	public boolean has(String channelId) {
		return _channels.containsKey(channelId);
	}
	
	public int getRole(String channelId) {
		if (this.has(channelId) == true) {
			return _channels.get(channelId).role;
		}
		return -1;
	}
	
	public int getAccess(String channelId) {
		if (this.has(channelId) == true) {
			return _channels.get(channelId).access;
		}
		return -1;
	}
	
	public int getInterval(String channelId) {
		return this.get(channelId).getInterval();
	}
	
	public void setAttributes(String channelId, int role, int access) {
		this.get(channelId).setInfo(channelId, role, access);
	}
	
	public boolean joined(String channelId) {
		if (this.has(channelId) == false) {
			return false;
		}
		return this.get(channelId).initialized();
	}
	
	
	public List<String> getChannelIds() {
		List<String> keys = new ArrayList<>();
		for (String key : _channels.keySet()) {
			keys.add(key);
		}
		return keys;
	}
	
	public JSONObject getChannels() {
		JSONObject channels = new JSONObject();
		for (String key : _channels.keySet()) {
			ChannelAttributes attrs = _channels.get(key);
			if (attrs.initialized() == false) {
				continue;
			}
			
			JSONObject info = new JSONObject();
			info.put("id", attrs.id);
			info.put("role", attrs.role);
			channels.put(key, info);
		}
		return channels;
	}
	
	
	private ChannelAttributes get(String channelId) {
		ChannelAttributes attr;
		if (this.has(channelId) == false) {
			attr = new ChannelAttributes();
			_channels.put(channelId, attr);
		} else {
			attr = _channels.get(channelId);
		}
		return attr;
	}
	
	
	class ChannelAttributes {

		private String id = "";
		private int role = -1;
		private int access = 0;
		
		public int active = 0;
		
		
		public void setInfo(String id, int role, int access) {
			this.id = id;
			this.role = role;
			this.access = access;
		}
		
		public boolean initialized() {
			return !this.id.equals("");
		}
		
		public int getInterval() {
			int interval = -1;
			if (this.role < 0) {
				interval = 3000;
			} else if (this.role == 0) {
				interval = 2000;
			} else if (this.role >= 8) {
				interval = 0;
			} else if ((this.role & 0x07) > 0) {
				interval = 1000;
			}
			return interval;
		}
	}
}
