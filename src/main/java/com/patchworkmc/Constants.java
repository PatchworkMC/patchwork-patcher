package com.patchworkmc;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class Constants {
	private Constants() {
	}

	public static final class EventBus {
		private EventBus() {
		}

		public static final String EVENT_BUS = "net/minecraftforge/eventbus/api/IEventBus";

		public static final String REGISTER_STATIC = "patchwork$registerStaticEventHandlers";
		public static final String REGISTER_STATIC_DESC = "(L" + EVENT_BUS + ";)V";
		public static final String REGISTER_INSTANCE = "patchwork$registerInstanceEventHandlers";
		public static String getRegisterInstanceDesc(String className) {
			return "(L" + className + ";L" + EVENT_BUS + ";)V";
		}
	}

	public static final class Lambdas {
		public static final Handle METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
				false);
		public static final Type OBJECT_METHOD_TYPE = Type.getMethodType("(Ljava/lang/Object;)V");
	}
}
