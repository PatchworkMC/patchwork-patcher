package com.patchworkmc.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.Patchwork;

public class EventHandlerScanner extends ClassVisitor {
	private static final String REGISTER_STATIC = "patchwork_registerStaticEventHandlers";
	private static final String REGISTER_INSTANCE = "patchwork_registerInstanceEventHandlers";
	private static final String METAFACTORY_OWNER = "java/lang/invoke/LambdaMetafactory";
	private static final String METAFACTORY_DESCRIPTOR = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
	private static final String EVENT_BUS = "net/minecraftforge/eventbus/api/IEventBus";

	private Consumer<EventBusSubscriber> subscriberConsumer;
	private Consumer<SubscribeEvent> subscribeEventConsumer;
	private List<SubscribeEvent> staticSubscribers;
	private String className;

	public EventHandlerScanner(ClassVisitor parent, Consumer<EventBusSubscriber> subscriberConsumer, Consumer<SubscribeEvent> subscribeEventConsumer) {
		super(Opcodes.ASM7, parent);

		this.subscriberConsumer = subscriberConsumer;
		this.subscribeEventConsumer = subscribeEvent -> {
			if ((subscribeEvent.getAccess() & Opcodes.ACC_STATIC) != 0) {
				staticSubscribers.add(subscribeEvent);
			} else {
				subscribeEventConsumer.accept(subscribeEvent);
			}
		};
		this.staticSubscribers = new ArrayList<>();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		this.className = name;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;")) {
			return new EventBusSubscriberHandler(subscriberConsumer);
		} else {
			return super.visitAnnotation(descriptor, visible);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals(REGISTER_STATIC) || name.equals(REGISTER_INSTANCE)) {
			throw new IllegalArgumentException("Class already contained a method named " + name + ", this name is reserved by Patchwork!");
		}

		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

		return new MethodScanner(parent, access, name, descriptor, signature);
	}

	@Override
	public void visitEnd() {
		// Write patchwork_registerStaticEventHandlers

		if (staticSubscribers.isEmpty()) {
			super.visitEnd();

			return;
		}

		Handle metafactory = new Handle(Opcodes.H_INVOKESTATIC, METAFACTORY_OWNER, "metafactory", METAFACTORY_DESCRIPTOR, false);
		MethodVisitor staticRegistrar = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, REGISTER_STATIC, "(L" + EVENT_BUS + ";)V", null, null);

		if (staticRegistrar != null) {
			staticRegistrar.visitCode();

			for (SubscribeEvent subscriber: staticSubscribers) {
				Handle handler = new Handle(Opcodes.H_INVOKEVIRTUAL, className, subscriber.getMethod(), subscriber.getMethodDescriptor(), false);

				staticRegistrar.visitVarInsn(Opcodes.ALOAD, 0);
				staticRegistrar.visitInvokeDynamicInsn("accept", "()Ljava/util/function/Consumer;", metafactory, Type.getMethodType("(Ljava/lang/Object;)V"), handler, Type.getMethodType(subscriber.getMethodDescriptor()));
				staticRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EVENT_BUS, "addListener", "(Ljava/util/function/Consumer;)V", true);
			}

			staticRegistrar.visitInsn(Opcodes.RETURN);
			staticRegistrar.visitMaxs(2, 1);
			staticRegistrar.visitEnd();
		}

		super.visitEnd();
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
			if (!annotation.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")) {
				return super.visitAnnotation(annotation, visible);
			}

			if ((access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE) {
				throw new IllegalArgumentException("Methods marked with @SubscribeEvent must not be private, but " + name + " has private access");
			}

			String eventClass;
			boolean hasReturnValue;

			// Make sure it's a void method
			if (descriptor.endsWith(")V")) {
				// Remove ( and )V
				eventClass = this.descriptor.substring(1, this.descriptor.length() - 2);
				hasReturnValue = false;
			} else {
				int end = this.descriptor.lastIndexOf(')');

				eventClass = this.descriptor.substring(1, end);
				hasReturnValue = true;
			}

			// If the string is now empty, that means that the original descriptor was ()V

			if (eventClass.isEmpty()) {
				throw new IllegalArgumentException("Methods marked with @SubscribeEvent must have one argument, but the method " + name + " had none (Descriptor: " + this.descriptor + ")");
			}

			// If the first occurrence of the ; is not equal to the last occurrence, that means that
			// there are multiple ; characters in the descriptor, and therefore multiple arguments
			// Or, if the descriptor does not start and end with L and ; that means that there are
			// primitive arguments

			if ((eventClass.indexOf(';') != eventClass.lastIndexOf(';')) || !eventClass.startsWith("L") || !eventClass.endsWith(";")) {
				throw new IllegalArgumentException("Methods marked with @SubscribeEvent must have only one argument, but the method " + name + " had multiple (Descriptor: " + this.descriptor + ")");
			}

			// Remove L and ;
			eventClass = eventClass.substring(1, eventClass.length() - 1);

			String genericClass = null;

			// Strip the generic class name from the signature
			if (this.signature != null) {
				genericClass = this.signature;
				int start = genericClass.indexOf('<');
				int end = genericClass.lastIndexOf('>');

				// Remove the non-generic parts
				genericClass = genericClass.substring(start + 1, end);

				int trailingGeneric = genericClass.indexOf('<');

				if (trailingGeneric != -1) {
					genericClass = genericClass.substring(0, trailingGeneric) + ";";
				}

				// If the first occurrence of the ; is not equal to the last occurrence, that means
				// that there are multiple ; characters in the descriptor, and therefore multiple
				// type arguments Or, if the descriptor does not start and end with L and ; that
				// means that there are primitive type arguments

				if (genericClass.contains("*")) {
					Patchwork.LOGGER.error("Error while parsing event handler: %s.%s(%s):", className, this.name, eventClass);
					Patchwork.LOGGER.error(" - FIXME: Not sure how to handle wildcards! We need to implement proper signature parsing: %s", genericClass);

					genericClass = null;
				} else if ((genericClass.indexOf(';') != genericClass.lastIndexOf(';')) || !genericClass.startsWith("L") || !genericClass.endsWith(";")) {
					Patchwork.LOGGER.error("Error while parsing event handler: %s.%s(%s):", className, this.name, eventClass);
					Patchwork.LOGGER.error(" - FIXME: Generic events may only have one type parameter, but %s uses an event with multiple (Signature: %s)", name, this.signature);

					genericClass = null;
				} else {
					// Remove L and ;
					genericClass = genericClass.substring(1, genericClass.length() - 1);
				}
			}

			return new SubscribeEventHandler(this.access, this.name, eventClass, genericClass, hasReturnValue, subscribeEventConsumer);
		}
	}
}
