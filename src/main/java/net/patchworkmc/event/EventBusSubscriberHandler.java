package net.patchworkmc.event;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.Patchwork;

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

		if (name.equals("modid")) {
			subscriber.targetModId = value.toString();
		} else {
			Patchwork.LOGGER.error("Unexpected EventBusSubscriber property: " + name + "->" + value);
		}
	}

	@Override
	public void visitEnum(final String name, final String descriptor, final String value) {
		super.visitEnum(name, descriptor, value);

		if (!name.equals("bus")) {
			Patchwork.LOGGER.error("Unexpected EventBusSubscriber enum property: " + name + "->" + descriptor + "::" + value);

			return;
		}

		if (!descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber$Bus;")) {
			Patchwork.LOGGER.error("Unexpected descriptor for EventBusSubscriber bus property, continuing anyways: " + descriptor);
		}

		if (value.equals("FORGE")) {
			subscriber.bus = EventBusSubscriber.Bus.FORGE;
		} else if (value.equals("MOD")) {
			subscriber.bus = EventBusSubscriber.Bus.MOD;
		} else {
			Patchwork.LOGGER.error("Unexpected EventBusSubscriber bus property value: " + value);
		}
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		if (name.equals("value")) {
			subscriber.client = false;
			subscriber.server = false;

			return new SideHandler(this.subscriber);
		} else {
			Patchwork.LOGGER.error("Unexpected EventBusSubscriber array property: " + name);
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

		SideHandler(EventBusSubscriber subscriber) {
			super(Opcodes.ASM7);

			this.subscriber = subscriber;
		}

		@Override
		public void visitEnum(final String name, final String descriptor, final String value) {
			super.visitEnum(name, descriptor, value);

			if (!descriptor.equals("Lnet/minecraftforge/api/distmarker/Dist;")) {
				Patchwork.LOGGER.error("Unexpected descriptor for EventBusSubscriber side property, continuing anyways: " + descriptor);
			}

			if (value.equals("CLIENT")) {
				subscriber.client = true;
			} else if (value.equals("SERVER")) {
				subscriber.server = true;
			}
		}
	}
}
