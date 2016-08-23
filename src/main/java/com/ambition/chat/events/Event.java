package com.ambition.chat.events;

import java.util.EventObject;

public class Event extends EventObject {

	public static final String COMPLETE = "complete";
	public static final String ERROR = "error";
	
	private static final long serialVersionUID = 1L;
	
	private String type = "";
	
	public Event(Object source, String type) {
		super(source);
		this.type = type;
	}
	
	public String type() {
		return this.type;
	}
	
	@Override
	public String toString() {
		return "[" + this.type() + "]";
	}
}
