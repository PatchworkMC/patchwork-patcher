package net.coderbot.patchwork.event;

import java.util.Optional;

public class SubscribeEvent {
	private int access;
	private String method;
	private String eventClass;
	private String genericClass;
	String priority;
	boolean receiveCancelled;

	public SubscribeEvent(int access, String method, String eventClass, String genericClass) {
		this.access = access;
		this.method = method;
		this.eventClass = eventClass;
		this.genericClass = genericClass;
		priority = "NORMAL";
		receiveCancelled = false;
	}

	public int getAccess() {
		return access;
	}

	public String getMethod() {
		return method;
	}

	public String getEventClass() {
		return eventClass;
	}

	public Optional<String> getGenericClass() {
		return Optional.ofNullable(genericClass);
	}

	public String getPriority() {
		return priority;
	}

	public boolean receiveCancelled() {
		return receiveCancelled;
	}

	@Override
	public String toString() {
		return "SubscribeEvent{"
				+ "access=" + access + ", method='" + method + '\'' + ", eventClass='" +
				eventClass + '\'' + ", genericClass='" + genericClass + '\'' + ", priority='" +
				priority + '\'' + ", receiveCancelled=" + receiveCancelled + '}';
	}
}
