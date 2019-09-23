package net.coderbot.patchwork.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.spongepowered.asm.lib.Opcodes;

public class SubscribeEvent {
	private String descriptor;
	private String signature;
	private String priority;
	private boolean receiveCancelled;

	public SubscribeEvent(String descriptor, String signature) {
		this.descriptor = descriptor;
		this.signature = signature;
		priority = "NORMAL";
		receiveCancelled = false;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public String getSignature() {
		return signature;
	}

	public String getPriority() {
		return priority;
	}

	public boolean receiveCancelled() {
		return receiveCancelled;
	}

	@Override
	public String toString() {
		return "SubscribeEvent{" +
				"descriptor='" + descriptor + '\'' +
				", signature='" + signature + '\'' +
				", priority='" + priority + '\'' +
				", receiveCancelled=" + receiveCancelled +
				'}';
	}

	public static class Handler extends AnnotationVisitor {
		SubscribeEvent instance;

		public Handler(String name, String descriptor, String signature, ForgeAnnotations target) {
			super(Opcodes.ASM7);

			instance = new SubscribeEvent(descriptor, signature);

			target.subscriptions.put(name, instance);
		}

		@Override
		public void visit(final String name, final Object value) {
			super.visit(name, value);

			// TODO: Not tested yet
			System.out.println(name + "->" + value);

			if (name.equals("receiveCancelled")) {
				instance.receiveCancelled = value == Boolean.TRUE;
			} else {
				System.err.println("Unexpected SubscribeEvent property: " + name + "->" + value);
			}
		}

		@Override
		public void visitEnum(final String name, final String descriptor, final String value) {
			super.visitEnum(name, descriptor, value);

			if(!name.equals("priority")) {
				System.err.println("Unexpected SubscribeEvent enum property: " + name + "->" + descriptor + "::" + value);

				return;
			}

			// TODO! not tested yet, wrong name to test
			if(!descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber$Bus;")) {
				System.out.println("Unexpected descriptor for SubscribeEvent bus property, continuing anyways: " + descriptor);
			}

			instance.priority = value;
		}
	}
}
