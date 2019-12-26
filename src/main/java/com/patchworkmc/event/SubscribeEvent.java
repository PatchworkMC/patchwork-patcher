package com.patchworkmc.event;

import java.util.Optional;

public class SubscribeEvent {
	String priority;
	boolean receiveCancelled;
	private int access;
	private String method;
	private String eventClass;
	private String genericClass;

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

	@Override
	public String toString() {
		return "SubscribeEvent{" + "access=" + access + ", method='" + method + '\'' + ", eventClass='" + eventClass + '\'' + ", genericClass='" + genericClass + '\'' + ", priority='" + priority + '\'' + ", receiveCancelled=" + receiveCancelled + '}';
	}
}
