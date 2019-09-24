package net.coderbot.patchwork.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class EventBusSubscriberHandler extends AnnotationVisitor {
	private AnnotationConsumer consumer;
	private boolean client;
	private boolean server;
	private String targetModId;
	private Bus bus;

	public EventBusSubscriberHandler(AnnotationConsumer consumer) {
		super(Opcodes.ASM7);

		this.consumer = consumer;
		this.client = true;
		this.server = true;
		this.bus = Bus.MOD;
	}

	@Override
	public void visit(final String name, final Object value) {
		super.visit(name, value);

		System.out.println(name + "->" + value);

		if (name.equals("modid")) {
			targetModId = value.toString();
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
			bus = Bus.FORGE;
		} else if(value.equals("MOD")) {
			bus = Bus.MOD;
		} else {
			System.err.println("Unexpected EventBusSubscriber bus property value: " + value);
		}
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		if(name.equals("value")) {
			client = false;
			server = false;

			return new SideHandler(this);

		} else {
			System.err.println("Unexpected EventBusSubscriber array property: " + name);
		}

		return super.visitArray(name);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		consumer.acceptEventBusSubscriber(targetModId, bus, client, server);
	}

	public enum Bus {
		FORGE, MOD;
	}

	static class SideHandler extends AnnotationVisitor {
		EventBusSubscriberHandler instance;

		public SideHandler(EventBusSubscriberHandler instance) {
			super(Opcodes.ASM7);

			this.instance = instance;
		}


		@Override
		public void visitEnum(final String name, final String descriptor, final String value) {
			super.visitEnum(name, descriptor, value);

			if(!descriptor.equals("Lnet/minecraftforge/api/distmarker/Dist;")) {
				System.out.println("Unexpected descriptor for EventBusSubscriber side property, continuing anyways: " + descriptor);
			}

			if(value.equals("CLIENT")) {
				instance.client = true;
			} else if(value.equals("SERVER")) {
				instance.server = true;
			}
		}
	}
}
