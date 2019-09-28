package net.coderbot.patchwork.generator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A utility for generating classes that implement java.util.Consumer<T>
 */
public class ConsumerGenerator {
	private ClassVisitor visitor;
	private String name;
	private String descriptor;
	private String signature;

	/**
	 * Creates a new ConsumerGenerator.
	 *
	 * @param visitor The class visitor that will be visited with the generated Consumer class
	 * @param name The name of the class to generate
	 * @param descriptor The descriptor of the type parameter (T in Consumer<T>), for example
	 *         "Ljava/lang/String;"
	 * @param signature The signature of the type parameter, for example
	 *         "Ljava/util/function/Consumer<Ljava/lang/String;>;"
	 */
	public ConsumerGenerator(ClassVisitor visitor, String name, String descriptor, String signature) {
		this.visitor = visitor;
		this.name = name;
		this.descriptor = descriptor;
		this.signature = signature;

		visitor.visit(Opcodes.V1_8,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
				name,
				"Ljava/lang/Object;Ljava/util/function/Consumer<" + (signature == null ? descriptor : signature) + ">;",
				"java/lang/Object",
				new String[] { "java/util/function/Consumer" });
	}

	public ClassVisitor getVisitor() {
		return this.visitor;
	}

	/**
	 * Visits a constructor method
	 *
	 * @return the method visitor corresponding to the constructor
	 */
	public MethodVisitor visitConstructor(String descriptor, String signature) {
		return visitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, signature, null);
	}

	/**
	 * Visits a constructor taking no arguments
	 *
	 * @return the method visitor corresponding to the constructor
	 */
	public MethodVisitor visitConstructor() {
		return visitConstructor("()V", null);
	}

	/**
	 * Visits the default constructor that simply calls the super constructor.
	 */
	public void visitDefaultConstructor() {
		MethodVisitor method = visitConstructor();

		method.visitVarInsn(Opcodes.ALOAD, 0);

		method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(1, 1);
		method.visitEnd();
	}

	/**
	 * Visits the "accept" method for the caller to fill in.
	 *
	 * @return a MethodVisitor to implement accept(T) within
	 */
	public MethodVisitor visitAccept() {
		return visitor.visitMethod(Opcodes.ACC_PUBLIC,
				"accept",
				"(" + descriptor + ")V",
				signature != null ? "(" + signature + ")V" : null,
				null);
	}

	/**
	 * Generates a bridge method and completes the class, calling visitEnd on the class visitor.
	 */
	public void visitEnd() {
		MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE,
				"accept",
				"(Ljava/lang/Object;)V",
				null,
				null);

		method.visitVarInsn(Opcodes.ALOAD, 0);

		method.visitVarInsn(Opcodes.ALOAD, 1);

		// CHECKCAST wants a type of the form "java/lang/String" so we strip the L and the ; from
		// the descriptor

		method.visitTypeInsn(Opcodes.CHECKCAST, descriptor.substring(1, descriptor.length() - 1));

		method.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL, name, "accept", "(" + descriptor + ")V", false);

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(2, 2);
		method.visitEnd();

		visitor.visitEnd();
	}
}
