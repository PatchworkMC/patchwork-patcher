package com.patchworkmc.event;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.Patchwork;
import com.patchworkmc.transformer.PatchworkTransformer;

public class EventHandlerScanner extends ClassVisitor {
	// TODO: LambdaConstants class
	public static final String EVENT_BUS = "net/minecraftforge/eventbus/api/IEventBus";

	public static final String REGISTER_STATIC = "patchwork$registerStaticEventHandlers";
	public static final String REGISTER_STATIC_DESC = "(L" + EVENT_BUS + ";)V";
	public static final String REGISTER_INSTANCE = "patchwork$registerInstanceEventHandlers";

	public static String getRegisterInstanceDesc(String className) {
		return "(L" + className + ";L" + EVENT_BUS + ";)V";
	}

	public static final Handle METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
		"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
		false);
	public static final Type OBJECT_METHOD_TYPE = Type.getMethodType("(Ljava/lang/Object;)V");


	private final Consumer<EventBusSubscriber> subscriberConsumer;
	private final Consumer<SubscribeEvent> subscribeEventConsumer;
	// boolean is "isStatic"
	private final HashMap<SubscribeEvent, Boolean> subscribeEvents = new HashMap<>();
	private String className;
	private boolean hasStatic = false;
	private boolean hasInstance = false;

	public EventHandlerScanner(ClassVisitor parent, Consumer<EventBusSubscriber> subscriberConsumer, Consumer<SubscribeEvent> subscribeEventConsumer) {
		super(Opcodes.ASM7, parent);

		this.subscriberConsumer = subscriberConsumer;
		this.subscribeEventConsumer = subscribeEvent -> {
			if ((subscribeEvent.getAccess() & Opcodes.ACC_STATIC) != 0) {
				subscribeEvents.put(subscribeEvent, true);
				hasStatic = true;
			} else {
				subscribeEvents.put(subscribeEvent, false);
				hasInstance = true;
			}
			subscribeEventConsumer.accept(subscribeEvent);
		};
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

	/**
	 * @deprecated glitch please clean this code up before merging it
	 */
	@Override
	@Deprecated
	public void visitEnd() {
		if (subscribeEvents.isEmpty()) {
			super.visitEnd();

			return;
		}

		MethodVisitor staticRegistrar = null;
		MethodVisitor instanceRegistrar = null;
		if (hasStatic) {
			staticRegistrar = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, REGISTER_STATIC, REGISTER_STATIC_DESC, null, null);
			staticRegistrar.visitCode();
		}

		if (hasInstance) {
			instanceRegistrar = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, REGISTER_INSTANCE, getRegisterInstanceDesc(className), null, null);
			instanceRegistrar.visitCode();
		}

		// There are additional null checks in here to get IDEA to shut up about impossible NPEs
		for (Map.Entry<SubscribeEvent, Boolean> subscribeEventBooleanEntry : subscribeEvents.entrySet()) {
			SubscribeEvent subscriber = subscribeEventBooleanEntry.getKey();
			boolean isStatic = subscribeEventBooleanEntry.getValue();
			if (isStatic && staticRegistrar != null) {
				Handle handler = new Handle(Opcodes.H_INVOKESTATIC, className, subscriber.getMethod(), subscriber.getMethodDescriptor(), false);
				staticRegistrar.visitVarInsn(Opcodes.ALOAD, 0); // Load IEventBus on to the stack (1)
				// Load the lambda on to the stack (2)
				staticRegistrar.visitInvokeDynamicInsn("accept", "()Ljava/util/function/Consumer;", METAFACTORY, OBJECT_METHOD_TYPE, handler, Type.getMethodType(subscriber.getMethodDescriptor()));
				// Pop eventbus and the lambda (0)
				staticRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EVENT_BUS, "addListener", "(Ljava/util/function/Consumer;)V", true);
			} else if (instanceRegistrar != null){
				// todo will no invokeinterace be a problem?
				instanceRegistrar.visitVarInsn(Opcodes.ALOAD, 1); // Load IEventBus on to the stack (1)
				instanceRegistrar.visitVarInsn(Opcodes.ALOAD, 0); // Load our target class (2)
				instanceRegistrar.visitInsn(Opcodes.DUP); // Duplicate it (3)
				// TODO: This is JDK8 javac's magic null-checking method.
				// TODO: We should use Objects.requireNonNull like java 9+
				// Null-check the target class (pop the instance, but return a value) (3)
				instanceRegistrar.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				instanceRegistrar.visitInsn(Opcodes.POP); // Pop the return value because we don't need it (2)
				// STACK:
				// (bottom comes out first)
				// IEventBus
				// <this>, essentially
				// TODO: better invokespecial
				Handle handler = new Handle(Opcodes.H_INVOKEVIRTUAL,
					className, subscriber.getMethod(), subscriber.getMethodDescriptor(), false);
				// Push the lambda (3)
				instanceRegistrar.visitInvokeDynamicInsn("accept", "(L" + this.className + ";)Ljava/util/function/Consumer;", METAFACTORY, OBJECT_METHOD_TYPE, handler, Type.getMethodType(subscriber.getMethodDescriptor()));
				// Pop the eventbus instance, the class, and the lambda (0)
				instanceRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EVENT_BUS, "addListener", "(Ljava/util/function/Consumer;)V", true);
			}

		}
		if (hasStatic && staticRegistrar != null) {
		staticRegistrar.visitInsn(Opcodes.RETURN);
		staticRegistrar.visitMaxs(2, 1);
		staticRegistrar.visitEnd();
		}

		if (hasInstance && instanceRegistrar != null) {
			instanceRegistrar.visitInsn(Opcodes.RETURN);
			instanceRegistrar.visitMaxs(3, 2);
			instanceRegistrar.visitEnd();
		}

		super.visitEnd();
	}


	public static void test(EventHandlerScanner x, PatchworkTransformer o) {
		o.accept(x::foo);
		o.accept(x::bar);
	}

	public void foo(String string) {

	}
	public void bar(String string) {

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
