package com.patchworkmc.event;

public final class EventConstants {
	private EventConstants() {
	}

	public static final String EVENT_BUS = "net/minecraftforge/eventbus/api/IEventBus";

	public static final String REGISTER_STATIC = "patchwork$registerStaticEventHandlers";
	public static final String REGISTER_STATIC_DESC = "(L" + EVENT_BUS + ";)V";
	public static final String REGISTER_INSTANCE = "patchwork$registerInstanceEventHandlers";

	public static String getRegisterInstanceDesc(String className) {
		return "(L" + className + ";L" + EVENT_BUS + ";)V";
	}
}
