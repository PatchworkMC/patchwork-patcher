package net.patchworkmc.patcher.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.event.initialization.EventMetaRegistrarGenerator;
import net.patchworkmc.patcher.transformer.ClassPostTransformer;
import net.patchworkmc.patcher.transformer.VisitorTransformer;
import net.patchworkmc.patcher.util.LambdaVisitors;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class EventHandlerRewriter extends VisitorTransformer {
	private final Consumer<SubscribeEvent> subscribeEventConsumer;

	private final HashSet<SubscribeEvent> staticSubscribeEvents = new HashSet<>();
	private final HashSet<SubscribeEvent> instanceSubscribeEvents = new HashSet<>();

	private String className;
	private String superName;
	private String[] interfaces;
	private EventBusSubscriber subscriber = null;
	private boolean isInterface = false;
	private boolean isFinalClass = false;

	public EventHandlerRewriter(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer widenings) {
		super(version, jar, parent, widenings);

		this.subscribeEventConsumer = subscribeEvent -> {
			if ((subscribeEvent.getAccess() & Opcodes.ACC_STATIC) != 0) {
				staticSubscribeEvents.add(subscribeEvent);
			} else {
				instanceSubscribeEvents.add(subscribeEvent);
			}
		};
	}

	private SubscribingClass asSubscribingClass() {
		return new SubscribingClass(className, isInterface, !instanceSubscribeEvents.isEmpty(), !staticSubscribeEvents.isEmpty());
	}

	// Needed for quoteall's EventSubscriptionChecker
	public List<SubscribeEvent> getSubscribeEvents() {
		ArrayList<SubscribeEvent> result = new ArrayList<>();
		result.addAll(staticSubscribeEvents);
		result.addAll(instanceSubscribeEvents);
		return result;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.superName = superName;
		this.interfaces = interfaces;

		if ((access & Opcodes.ACC_INTERFACE) != 0) {
			this.isInterface = true;
		}

		if ((access & Opcodes.ACC_FINAL) != 0) {
			if (this.isInterface) {
				throw new AssertionError("Mod has a final interface!");
			}

			this.isFinalClass = true;
		}

		this.className = name;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals("Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;")) {
			return new EventBusSubscriberHandler(subscriber -> this.subscriber = subscriber);
		} else {
			return super.visitAnnotation(descriptor, visible);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals(EventConstants.REGISTER_STATIC) || name.equals(EventConstants.REGISTER_INSTANCE)
				|| name.equals(EventConstants.REGISTER_META)) {
			throw new IllegalArgumentException("Class already contained a method named " + name + ", this name is reserved by Patchwork!");
		}

		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

		return new MethodScanner(parent, access, name, descriptor, signature);
	}

	@Override
	public void visitEnd() {
		genStaticRegistrar();
		genInstanceRegistrar();

		// TODO: this might not be needed
		this.postTransformer.makeClassPublic();
		this.genMetaRegistrar();

		List<String> supers = new ArrayList<>(2);
		supers.add(superName);
		supers.addAll(Arrays.asList(interfaces));
		this.postTransformer.registerToInheritenceChecker(this.className, this.getSubscribeEvents(), supers);

		super.visitEnd();
	}

	private void genMetaRegistrar() {
		if (staticSubscribeEvents.isEmpty() && instanceSubscribeEvents.isEmpty()) {
			return;
		}

		// TODO: this should be properly sided
		MethodVisitor metaRegistrar = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
				EventConstants.REGISTER_META, "()V", null, null);
		EventMetaRegistrarGenerator.accept(metaRegistrar, this.subscriber, this.asSubscribingClass());

		// TODO: don't assume the modid
		this.forgeModJar.addEntrypoint("patchwork:commonAutomaticSubscribers", this.className + "::" + EventConstants.REGISTER_META);
	}

	private void genStaticRegistrar() {
		if (staticSubscribeEvents.isEmpty()) {
			return;
		}

		MethodVisitor staticRegistrar = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, EventConstants.REGISTER_STATIC, EventConstants.REGISTER_STATIC_DESC, null, null);

		if (staticRegistrar == null) {
			throw new IllegalStateException("Parent scanner threw out generated method for static registrar!");
		}

		staticRegistrar.visitCode();

		for (SubscribeEvent subscriber : staticSubscribeEvents) {
			boolean isGeneric = subscriber.getGenericClass().isPresent();
			staticRegistrar.visitVarInsn(Opcodes.ALOAD, 0); // Load IEventBus on to the stack (1)
			loadParameters(staticRegistrar, subscriber); // other parameters (5)
			// Load the Consumer onto the stack (6)
			LambdaVisitors.visitConsumerStaticLambda(staticRegistrar, className, subscriber.getMethod(), subscriber.getMethodDescriptor(), isInterface);
			// Pop eventbus and the Consumer

			if (isGeneric) {
				staticRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EventConstants.EVENT_BUS, "addGenericListener",
						"(Ljava/lang/Class;Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V", true);
			} else {
				staticRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EventConstants.EVENT_BUS, "addListener",
						"(Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V", true);
			}
		}

		staticRegistrar.visitInsn(Opcodes.RETURN);
		staticRegistrar.visitMaxs(6, 1);
		staticRegistrar.visitEnd();
	}

	private void genInstanceRegistrar() {
		if (instanceSubscribeEvents.isEmpty()) {
			return;
		}

		MethodVisitor instanceRegistrar = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, EventConstants.REGISTER_INSTANCE, EventConstants.getRegisterInstanceDesc(className), null, null);

		if (instanceRegistrar == null) {
			throw new IllegalStateException("Parent scanner threw out generated method for static registrar!");
		}

		instanceRegistrar.visitCode();

		// Check the target instance isn't null.
		// TODO: this potentially causes differing bytecode when recompiled, because
		// TODO: javac will insert this check for every lambda and some decompilers (cough fernflower) don't properly detect the J9 method.

		instanceRegistrar.visitVarInsn(Opcodes.ALOAD, 0); // Load the target instance for null-checking (1)
		// In every java version this is run for each INVOKEDYNAMIC but we only do it once here.
		// In java 8, this is a INVOKEVIRTUAL Object.getClass but we use the Java 9 way here instead because it makes more sense
		// pops the instance and instantly returns it (1)
		instanceRegistrar.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		instanceRegistrar.visitInsn(Opcodes.POP); // pop the returned instance (0)

		// Register the subscribers

		for (SubscribeEvent subscriber : instanceSubscribeEvents) {
			instanceRegistrar.visitVarInsn(Opcodes.ALOAD, 1); // Load IEventBus on to the stack (1)
			loadParameters(instanceRegistrar, subscriber); // (5)

			int callingOpcode;

			if (isInterface) {
				callingOpcode = Opcodes.H_INVOKEINTERFACE;
			} else if (isFinalClass || (subscriber.getAccess() & Opcodes.ACC_FINAL) != 0) {
				callingOpcode = Opcodes.H_INVOKESPECIAL;
			} else {
				callingOpcode = Opcodes.H_INVOKEVIRTUAL;
			}

			instanceRegistrar.visitVarInsn(Opcodes.ALOAD, 0); // Load the target instance (6)
			// Swap the target instance with a Consumer instance (6)
			LambdaVisitors.visitConsumerInstanceLambda(instanceRegistrar, callingOpcode, className, subscriber.getMethod(), subscriber.getMethodDescriptor(), isInterface);

			// Pop the eventbus, params, and the lambda. (0)
			if (subscriber.getGenericClass().isPresent()) {
				instanceRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EventConstants.EVENT_BUS, "addGenericListener",
						"(Ljava/lang/Class;Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V", true);
			} else {
				instanceRegistrar.visitMethodInsn(Opcodes.INVOKEINTERFACE, EventConstants.EVENT_BUS, "addListener",
						"(Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V", true);
			}
		}

		instanceRegistrar.visitInsn(Opcodes.RETURN);
		instanceRegistrar.visitMaxs(6, 2);
		instanceRegistrar.visitEnd();
	}

	/**
	 * Load all parameters excluding the IEventBus instance and the final consumer.
	 * <p>
	 * Pushes either 3 or 4 elements onto the stack depending on if there is a generic.
	 * </p>
	 */
	private void loadParameters(MethodVisitor method, SubscribeEvent subscriber) {
		subscriber.getGenericClass().ifPresent(g -> method.visitLdcInsn(Type.getObjectType(g))); // Possibly load the generic class (1)

		method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/eventbus/api/EventPriority", subscriber.getPriority(),
				"Lnet/minecraftforge/eventbus/api/EventPriority;"); // Load the priority (2)
		method.visitInsn(subscriber.receiveCancelled() ? Opcodes.ICONST_1 : Opcodes.ICONST_0); // load if canceled events should be recieved (3)
		method.visitLdcInsn(Type.getObjectType(subscriber.getEventClass())); // Load the event class (4)
	}

	public class MethodScanner extends MethodVisitor {
		int access;
		String name;
		String descriptor;
		String signature;

		MethodScanner(MethodVisitor parent, int access, String name, String descriptor, String signature) {
			super(Opcodes.ASM9, parent);

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
