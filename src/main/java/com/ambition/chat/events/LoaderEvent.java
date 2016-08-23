package com.ambition.chat.events;

import org.apache.http.HttpResponse;

public class LoaderEvent extends Event {

	public static final String CANCEL = "cancel";
	
	private static final long serialVersionUID = 3L;
	
	private HttpResponse response = null;
	
	public LoaderEvent(Object source, String type) {
		this(source, type, null);
	}
	
	public LoaderEvent(Object source, String type, HttpResponse response) {
		super(source, type);
		this.response = response;
	}
	
	public HttpResponse response() {
		return this.response;
	}
	
	@Override
	public String toString() {
		return "[" + this.type() + " data=" + this.response.toString() + "]";
	}
}
