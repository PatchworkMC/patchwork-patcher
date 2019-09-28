package net.coderbot.patchwork.event;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class EventBusSubscriberHandler extends AnnotationVisitor {
	private Consumer<EventBusSubscriber> consumer;
	private EventBusSubscriber subscriber;

	public EventBusSubscriberHandler(Consumer<EventBusSubscriber> consumer) {
		super(Opcodes.ASM7);

		this.consumer = consumer;
		this.subscriber = new EventBusSubscriber();
	}

	@Override
	public void visit(final String name, final Object value) {
		super.visit(name, value);

		if(name.equals("modid")) {
			subscriber.targetModId = value.toString();
		} else {
			System.err.println("Unexpected EventBusSubscriber property: " + name + "->" + value);
		}
	}

	@Override
	public void visitEnum(final String name, final String descriptor, final String value) {
		super.visitEnum(name, descriptor, value);

		if(!name.equals("bus")) {
			System.err.println("Unexpected EventBusSubscriber enum property: " + name + "->" +
							   descriptor + "::" + value);

			return;
		}

		if(!descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber$Bus;")) {
			System.err.println(
					"Unexpected descriptor for EventBusSubscriber bus property, continuing anyways: " +
					descriptor);
		}

		if(value.equals("FORGE")) {
			subscriber.bus = EventBusSubscriber.Bus.FORGE;
		} else if(value.equals("MOD")) {
			subscriber.bus = EventBusSubscriber.Bus.MOD;
		} else {
			System.err.println("Unexpected EventBusSubscriber bus property value: " + value);
		}
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		if(name.equals("value")) {
			subscriber.client = false;
			subscriber.server = false;

			return new SideHandler(this.subscriber);

		} else {
			System.err.println("Unexpected EventBusSubscriber array property: " + name);
		}

		return super.visitArray(name);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		consumer.accept(subscriber);
	}

	static class SideHandler extends AnnotationVisitor {
		EventBusSubscriber subscriber;

		public SideHandler(EventBusSubscriber subscriber) {
			super(Opcodes.ASM7);

			this.subscriber = subscriber;
		}

		@Override
		public void visitEnum(final String name, final String descriptor, final String value) {
			super.visitEnum(name, descriptor, value);

			if(!descriptor.equals("Lnet/minecraftforge/api/distmarker/Dist;")) {
				System.err.println(
						"Unexpected descriptor for EventBusSubscriber side property, continuing anyways: " +
						descriptor);
			}

			if(value.equals("CLIENT")) {
				subscriber.client = true;
			} else if(value.equals("SERVER")) {
				subscriber.server = true;
			}
		}
	}
}
