package com.ambition.chat.events;

public interface EventListener extends java.util.EventListener {

	public abstract void callback(Event e);
}
