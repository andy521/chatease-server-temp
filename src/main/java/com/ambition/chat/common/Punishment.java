package com.ambition.chat.common;

public class Punishment {

	private String ip = null;
	private String channelId = null;
	private int id = 0;
	private int code = 0;
	private long time = 0;
	
	public Punishment(String ip, int code, long time) {
		// TODO Auto-generated constructor stub
		this.init(ip, null, 0, code, time);
	}
	
	public Punishment(int id, int code, long time) {
		// TODO Auto-generated constructor stub
		this.init(null, null, id, code, time);
	}
	
	public Punishment(String channelId, int id, int code, long time) {
		// TODO Auto-generated constructor stub
		this.init(null, channelId, id, code, time);
	}
	
	private void init(String ip, String channelId, int id, int code, long time) {
		// TODO Auto-generated constructor stub
		this.ip = ip;
		this.channelId = channelId;
		this.id = id;
		this.code = code;
		this.time = time;
	}
	
	public String getIp() {
		return this.ip;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getChannelId() {
		return this.channelId;
	}
	
	public int getCode() {
		return this.code;
	}
	
	public long getTime() {
		return this.time;
	}
}
