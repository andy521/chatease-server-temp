package com.ambition.chat.common;

import java.util.Date;

import org.json.JSONObject;

import com.ambition.chat.utils.Utils;

public class UserInfoInChannel {

	private String id = "";
	
	/**
	 * <ul>
	 * <li>-1: visitor</li>
	 * <li>0: signed</li>
	 * <li>1-7: VIP</li>
	 * <li>8: temporary</li>
	 * <li>16: manager</li>
	 * <li>32: owner</li>
	 * <li>64: system</li>
	 * <li>128: administrator</li>
	 * </ul>
	 */
	private int role = -1;
	
	/**
	 * <ul>
	 * <li>0: forbidden</li>
	 * <li>1: read</li>
	 * <li>3: write</li>
	 * </ul>
	 */
	private int state = 0;
	
	private int interval = -1;
	
	/**
	 * Number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	private int active = 0;
	private boolean joined = false;
	
	
	public UserInfoInChannel(String id) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.interval = Utils.getIntervalByRole(this.role);
	}
	
	public String getId() {
		return this.id;
	}
	
	public int getRole() {
		return this.role;
	}
	
	public int getState() {
		return this.state;
	}
	
	public int getInterval() {
		return this.interval;
	}
	
	public void setProperties(int role, int state) {
		this.role = role;
		this.state = state;
		this.interval = Utils.getIntervalByRole(this.role);
		this.joined = true;
	}
	
	public boolean setActive() {
		int current = ((Number) new Date().getTime()).intValue();
		if (this.active > 0) {
			if (this.interval < 0 || current - this.active < this.interval) {
				return false;
			}
		}
		this.active = current;
		return true;
	}
	
	public boolean joined() {
		return this.joined;
	}
	
	public JSONObject getJson() {
		JSONObject channel = new JSONObject();
		channel.put("id", this.id);
		channel.put("role", this.role);
		channel.put("state", this.state);
		return channel;
	}

}
