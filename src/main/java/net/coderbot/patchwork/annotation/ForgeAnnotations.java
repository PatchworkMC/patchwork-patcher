package net.coderbot.patchwork.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ForgeAnnotations {
	Mod mod;
	String objectHolderModId;
	EventBusSubscriber subscriber;
	Map<String, SubscribeEvent> subscriptions;
	Map<String, ObjectHolder> objectHolders;

	ForgeAnnotations() {
		mod = null;
		subscriber = null;
		subscriptions = new HashMap<>();
		objectHolders = new HashMap<>();
	}

	public Optional<Mod> getMod() {
		return Optional.ofNullable(mod);
	}

	public Optional<String> getObjectHolderModId() {
		return Optional.ofNullable(objectHolderModId);
	}

	public Optional<EventBusSubscriber> getSubscriber() {
		return Optional.ofNullable(subscriber);
	}

	public Map<String, SubscribeEvent> getSubscriptions() {
		return Collections.unmodifiableMap(subscriptions);
	}

	public Map<String, ObjectHolder> getObjectHolders() {
		return objectHolders;
	}
}
