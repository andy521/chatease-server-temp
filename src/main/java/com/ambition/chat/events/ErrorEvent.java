package com.ambition.chat.events;

public class ErrorEvent extends Event {

	private static final long serialVersionUID = 2L;
	
	private String explain = "";
	
	public ErrorEvent(Object source, String type) {
		// TODO Auto-generated constructor stub
		this(source, type, "");
	}
	
	public ErrorEvent(Object source, String type, String explain) {
		super(source, type);
		this.explain = explain;
	}
	
	public String explain() {
		return this.explain;
	}
	
	@Override
	public String toString() {
		return "[" + this.type() + " explain=" + this.explain() + "]";
	}
}
