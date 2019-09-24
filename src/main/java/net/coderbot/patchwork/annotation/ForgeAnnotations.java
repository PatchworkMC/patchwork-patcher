package net.coderbot.patchwork.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ForgeAnnotations {
	Map<String, SubscribeEvent> subscriptions;

	ForgeAnnotations() {
		subscriptions = new HashMap<>();
	}

	public Map<String, SubscribeEvent> getSubscriptions() {
		return Collections.unmodifiableMap(subscriptions);
	}
}
