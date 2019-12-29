package com.patchworkmc.event;

import java.util.Optional;

public class SubscribeEvent {
	String priority;
	boolean receiveCancelled;
	private int access;
	private String method;
	private String eventClass;
	private String genericClass;
	private boolean hasReturnValue;

	public SubscribeEvent(int access, String method, String eventClass, String genericClass, boolean hasReturnValue) {
		this.access = access;
		this.method = method;
		this.eventClass = eventClass;
		this.genericClass = genericClass;
		this.hasReturnValue = hasReturnValue;
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

	public String getMethodDescriptor() {
		return "(L" + getEventClass() + ";)V";
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

	public boolean hasReturnValue() {
		return hasReturnValue;
	}

	@Override
	public String toString() {
		return "SubscribeEvent{"
				+ "priority='" + priority + '\''
				+ ", receiveCancelled=" + receiveCancelled
				+ ", access=" + access
				+ ", method='" + method + '\''
				+ ", eventClass='" + eventClass + '\''
				+ ", genericClass='" + genericClass + '\''
				+ ", hasReturnValue=" + hasReturnValue
				+ '}';
	}
}
