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
		public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
			if(!annotation.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")) {
				return super.visitAnnotation(annotation, visible);
			}

			// Make sure it's a void method
			if(!descriptor.endsWith(")V")) {
				throw new IllegalArgumentException("Methods marked with @SubscribeEvent must return void, but " + name + " does not (Descriptor: " + this.descriptor + ")");
			}

			// Remove ( and )V
			String descriptor = this.descriptor.substring(1, this.descriptor.length() - 2);

			// If the first occurrence of the ; is not equal to the last occurrence, that means that there are multiple
			// ; characters in the descriptor, and therefore multiple arguments
			// Or, if the descriptor does not start and end with L and ; that means that there are primitive arguments

			if((descriptor.indexOf(';') != descriptor.lastIndexOf(';')) || !descriptor.startsWith("L") || !descriptor.endsWith(";")) {
				throw new IllegalArgumentException(
						"Methods marked with @SubscribeEvent must have only one argument, but the method " +
						name + " had multiple (Descriptor: " + this.descriptor + ")");
			}

			descriptor = descriptor.substring(1, descriptor.length() - 1);

			String signature = null;

			if (this.signature != null) {
				signature = this.signature;
				int start = signature.indexOf('<');
				int end = signature.lastIndexOf('>');

				// Remove the parts around the <> and the L and ; in one go
				signature = signature.substring(start + 2, end - 1);

				int trailingGeneric = signature.indexOf('<');

				if (trailingGeneric != -1) {
					signature = signature.substring(0, trailingGeneric);
				}
			}


			System.out.println(descriptor + " " + signature);

			// TODO: Verify the method descriptor, then grab the class name

			return new SubscribeEventHandler(this.access, this.name, this.descriptor, this.signature, subscribeEventConsumer);
		}
	}
}
