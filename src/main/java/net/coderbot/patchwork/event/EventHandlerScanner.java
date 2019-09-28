package net.coderbot.patchwork.event;

import java.util.function.Consumer;

import org.objectweb.asm.*;

public class EventHandlerScanner extends ClassVisitor {

	private Consumer<EventBusSubscriber> subscriberConsumer;
	private Consumer<SubscribeEvent> subscribeEventConsumer;

	public EventHandlerScanner(ClassVisitor parent,
			Consumer<EventBusSubscriber> subscriberConsumer,
			Consumer<SubscribeEvent> subscribeEventConsumer) {
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
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

		return new MethodScanner(parent, access, name, descriptor, signature);
	}

	public class MethodScanner extends MethodVisitor {
		int access;
		String name;
		String descriptor;
		String signature;

		MethodScanner(MethodVisitor parent,
				int access,
				String name,
				String descriptor,
				String signature) {
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

			if((access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE) {
				throw new IllegalArgumentException(
						"Methods marked with @SubscribeEvent must not be private, but " + name +
						" has private access");
			}

			// Make sure it's a void method
			if(!descriptor.endsWith(")V")) {
				throw new IllegalArgumentException(
						"Methods marked with @SubscribeEvent must return void, but " + name +
						" does not (Descriptor: " + this.descriptor + ")");
			}

			// Remove ( and )V
			String eventClass = this.descriptor.substring(1, this.descriptor.length() - 2);

			// If the string is now empty, that means that the original descriptor was ()V

			if(eventClass.isEmpty()) {
				throw new IllegalArgumentException(
						"Methods marked with @SubscribeEvent must have one argument, but the method " +
						name + " had none (Descriptor: " + this.descriptor + ")");
			}

			// If the first occurrence of the ; is not equal to the last occurrence, that means that
			// there are multiple ; characters in the descriptor, and therefore multiple arguments
			// Or, if the descriptor does not start and end with L and ; that means that there are
			// primitive arguments

			if((eventClass.indexOf(';') != eventClass.lastIndexOf(';')) ||
					!eventClass.startsWith("L") || !eventClass.endsWith(";")) {
				throw new IllegalArgumentException(
						"Methods marked with @SubscribeEvent must have only one argument, but the method " +
						name + " had multiple (Descriptor: " + this.descriptor + ")");
			}

			// Remove L and ;
			eventClass = eventClass.substring(1, eventClass.length() - 1);

			String genericClass = null;

			// Strip the generic class name from the signature
			if(this.signature != null) {
				genericClass = this.signature;
				int start = genericClass.indexOf('<');
				int end = genericClass.lastIndexOf('>');

				// Remove the non-generic parts
				genericClass = genericClass.substring(start + 1, end);

				int trailingGeneric = genericClass.indexOf('<');

				if(trailingGeneric != -1) {
					genericClass = genericClass.substring(0, trailingGeneric) + ";";
				}

				// If the first occurrence of the ; is not equal to the last occurrence, that means
				// that there are multiple ; characters in the descriptor, and therefore multiple
				// type arguments Or, if the descriptor does not start and end with L and ; that
				// means that there are primitive type arguments

				if((genericClass.indexOf(';') != genericClass.lastIndexOf(';')) ||
						!genericClass.startsWith("L") || !genericClass.endsWith(";")) {
					throw new IllegalArgumentException(
							"Generic events may only have one type parameter, but " + name +
							" uses an event with multiple (Signature: " + this.signature + ")");
				}

				// Remove L and ;
				genericClass = genericClass.substring(1, genericClass.length() - 1);
			}

			return new SubscribeEventHandler(
					this.access, this.name, eventClass, genericClass, subscribeEventConsumer);
		}
	}
}
