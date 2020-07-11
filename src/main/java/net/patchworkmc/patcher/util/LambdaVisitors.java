package net.patchworkmc.patcher.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class LambdaVisitors {
	private LambdaVisitors() {
	}

	public static final Handle METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
			false);
	public static final Type OBJECT_METHOD_TYPE = Type.getMethodType("(Ljava/lang/Object;)V");
	public static final Type DUAL_OBJECT_METHOD_TYPE = Type.getMethodType("(Ljava/lang/Object;Ljava/lang/Object;)V");

	/**
	 * Generates an {@code INVOKEDYNAMIC} instruction for a {@link Consumer} that is a reference to an instance method.
	 * <p>
	 * On the stack, this will pop the instance (which should be of type {@code className}) and push a {@link Consumer}.
	 * </p>
	 *
	 * @param visitor the {@link MethodVisitor} for the {@code INVOKEDYNAMIC} instruction to be written to.
	 * @param callingOpcode the opcode needed to invoke the method. The calling opcode *must* be a {@code H_INVOKE*} instruction.
	 * @param className the class that holds the target method
	 * @param methodName the target method's name
	 * @param methodDescriptor the target method's descriptor
	 * @param isInterface whether the target class is an interface or not
	 */
	public static void visitConsumerInstanceLambda(MethodVisitor visitor, int callingOpcode, String className, String methodName, String methodDescriptor, boolean isInterface) {
		if (callingOpcode < 1 || callingOpcode > 9) {
			throw new IllegalArgumentException("Expected a valid H_INVOKE opcode, got " + callingOpcode);
		}

		Handle handle = new Handle(callingOpcode, className, methodName, methodDescriptor, isInterface);
		visitor.visitInvokeDynamicInsn("accept", "(L" + className + ";)Ljava/util/function/Consumer;", METAFACTORY, OBJECT_METHOD_TYPE, handle, Type.getMethodType(methodDescriptor));
	}

	/**
	 * Generates an {@code INVOKEDYNAMIC} instruction for a {@link Consumer} that is a reference to a static method.
	 *
	 * @param visitor the {@link MethodVisitor} for the {@code INVOKEDYNAMIC} instruction to be written to.
	 * @param className the class that holds the target method
	 * @param methodName the target method's name
	 * @param methodDescriptor the target method's descriptor
	 * @param isInterface whether the target class is an interface or not
	 */
	public static void visitConsumerStaticLambda(MethodVisitor visitor, String className, String methodName, String methodDescriptor, boolean isInterface) {
		Handle handle = new Handle(Opcodes.H_INVOKESTATIC, className, methodName, methodDescriptor, isInterface);
		visitor.visitInvokeDynamicInsn("accept", "()Ljava/util/function/Consumer;", METAFACTORY, OBJECT_METHOD_TYPE, handle, Type.getMethodType(methodDescriptor));
	}

	/**
	 * Generates an {@code INVOKEDYNAMIC} instruction for a {@link BiConsumer} that is a reference to a static method.
	 *
	 * @param visitor the {@link MethodVisitor} for the {@code INVOKEDYNAMIC} instruction to be written to.
	 * @param className the class that holds the target method
	 * @param methodName the target method's name
	 * @param methodDescriptor the target method's descriptor
	 * @param isInterface whether the target class is an interface or not
	 */
	public static void visitBiConsumerStaticLambda(MethodVisitor visitor, String className, String methodName, String methodDescriptor, boolean isInterface) {
		Handle handle = new Handle(Opcodes.H_INVOKESTATIC, className, methodName, methodDescriptor, isInterface);
		visitor.visitInvokeDynamicInsn("accept", "()Ljava/util/function/BiConsumer;", METAFACTORY, DUAL_OBJECT_METHOD_TYPE, handle, Type.getMethodType(methodDescriptor));
	}
}
