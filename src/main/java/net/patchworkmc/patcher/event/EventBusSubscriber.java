package net.patchworkmc.patcher.event;

import org.jetbrains.annotations.Nullable;

/**
 * A representation of an @Mod.EventBusSubscriber annotation.
 */
public final class EventBusSubscriber {
	@Nullable
	protected String targetModId;
	protected boolean client;
	protected boolean server;
	protected Bus bus;

	EventBusSubscriber() {
		this(null, true, true, Bus.FORGE);
	}

	public EventBusSubscriber(@Nullable String targetModId, boolean client, boolean server, Bus bus) {
		this.targetModId = targetModId;
		this.client = client;
		this.server = server;
		this.bus = bus;
	}

	/**
	 * Note this is optional for some reason. If you don't define one it's essentially random,
	 * Patchwork defaults to the mod id it assigned as the parent.
	 */
	@Nullable
	public String getTargetModId() {
		return targetModId;
	}

	/**
	 * @return true if the class should be subscribed on the physical client
	 */
	public boolean isClient() {
		return client;
	}

	/**
	 * @return true if the class should be subscribed on the physical server
	 */
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
		MOD
	}
}
