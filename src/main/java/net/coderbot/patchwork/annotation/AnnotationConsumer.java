package net.coderbot.patchwork.annotation;

public interface AnnotationConsumer {
	void acceptMod(String modId);

	void acceptEventBusSubscriber(String modId, EventBusSubscriberHandler.Bus bus, boolean client, boolean server);
	void acceptSubscribeEvent(String method, String priority, boolean receiveCancelled);
}
