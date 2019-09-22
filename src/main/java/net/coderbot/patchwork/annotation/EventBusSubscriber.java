package net.coderbot.patchwork.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.spongepowered.asm.lib.Opcodes;

public class EventBusSubscriber {
	private boolean client;
	private boolean server;
	private String targetModId;
	private Bus bus;

	private EventBusSubscriber() {
		client = true;
		server = true;
		bus = Bus.FORGE;
	}

	public enum Bus {
		FORGE, MOD;
	}

	public static class Handler extends AnnotationVisitor {
		EventBusSubscriber instance;

		public Handler(ForgeAnnotations target) {
			super(Opcodes.ASM7);

			instance = new EventBusSubscriber();

			target.subscriber = instance;
		}

		@Override
		public void visit(final String name, final Object value) {
			super.visit(name, value);

			System.out.println(name + "->" + value);

			if (name.equals("modid")) {
				instance.targetModId = value.toString();
			} else {
				System.err.println("Unexpected EventBusSubscriber property: " + name + "->" + value);
			}
		}

		@Override
		public void visitEnum(final String name, final String descriptor, final String value) {
			super.visitEnum(name, descriptor, value);

			if(!name.equals("bus")) {
				System.err.println("Unexpected EventBusSubscriber enum property: " + name + "->" + descriptor + "::" + value);

				return;
			}

			if(!descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber$Bus;")) {
				System.out.println("Unexpected descriptor for EventBusSubscriber bus property, continuing anyways: " + descriptor);
			}

			if(value.equals("FORGE")) {
				instance.bus = Bus.FORGE;
			} else if(value.equals("MOD")) {
				instance.bus = Bus.MOD;
			} else {
				System.err.println("Unexpected EventBusSubscriber bus property value: " + value);
			}
		}

		@Override
		public AnnotationVisitor visitArray(final String name) {
			if(name.equals("value")) {
				// TODO

				System.err.println("Can't yet handle EventBusSubscriber side property");
			} else {
				System.err.println("Unexpected EventBusSubscriber array property: " + name);
			}

			return super.visitArray(name);
		}
	}
}
