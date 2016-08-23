package com.ambition.chat.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispathcer {

	private final Map<String, List<EventListener>> listeners = new HashMap<>();
	
	public final synchronized void addEventListener(String type, EventListener listener) {
		List<EventListener> list = listeners.get(type);
		if (list == null) {
			list = new ArrayList<>();
			listeners.put(type, list);
		}
		list.add(listener);
	}
	
	public final synchronized void removeEventListener(String type, EventListener listener) {
		List<EventListener> list = listeners.get(type);
		if (list == null) {
			return;
		}
		list.remove(listener);
	}
	
	public final synchronized void dispatchEvent(Event e) {
		List<EventListener> list = listeners.get(e.type());
		if (list == null) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			EventListener listener = list.get(i);
			listener.callback(e);
		}
	}
}
