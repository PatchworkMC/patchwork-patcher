package net.coderbot.patchwork.event;

import org.objectweb.asm.*;

import java.util.function.Consumer;

public class EventHandlerScanner extends ClassVisitor {

	private Consumer<EventBusSubscriber> subscriberConsumer;
	private Consumer<SubscribeEvent> subscribeEventConsumer;

	public EventHandlerScanner(ClassVisitor parent, Consumer<EventBusSubscriber> subscriberConsumer, Consumer<SubscribeEvent> subscribeEventConsumer) {
		super(Opcodes.ASM7, parent);

		this.subscriberConsumer = subscriberConsumer;
		this.subscribeEventConsumer = subscribeEventConsumer;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if(descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;")) {
			return new EventBusSubscriberHandler(subscriberConsumer);
		} else {
			return super.visitAnnotation(descriptor, visible);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

		return new MethodScanner(parent, access, name, descriptor, signature);
	}

	public class MethodScanner extends MethodVisitor {
		int access;
		String name;
		String descriptor;
		String signature;

		MethodScanner(MethodVisitor parent, int access, String name, String descriptor, String signature) {
			super(Opcodes.ASM7, parent);

			this.access = access;
			this.name = name;
			this.descriptor = descriptor;
			this.signature = signature;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if(descriptor.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")) {
				return new SubscribeEventHandler(this.access, this.name, this.descriptor, this.signature, subscribeEventConsumer);
			} else {
				return super.visitAnnotation(descriptor, visible);
			}
		}
	}
}
