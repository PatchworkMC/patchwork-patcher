package com.patchworkmc.event;

public class EventBusSubscriber {
	String targetModId;
	boolean client;
	boolean server;
	Bus bus;

	EventBusSubscriber() {
		this(null, true, true, Bus.FORGE);
	}

	public EventBusSubscriber(String targetModId, boolean client, boolean server, Bus bus) {
		this.targetModId = targetModId;
		this.client = client;
		this.server = server;
		this.bus = bus;
	}

	public String getTargetModId() {
		return targetModId;
	}

	public boolean isClient() {
		return client;
	}

	public boolean isServer() {
		return server;
	}

	public Bus getBus() {
		return bus;
	}

	@Override
	public String toString() {
		return "EventBusSubscriber{" + "targetModId='" + targetModId + '\'' + ", server=" + server + ", client=" + client + ", bus=" + bus + '}';
	}

	public enum Bus {
		FORGE,
		MOD;
	}
}
