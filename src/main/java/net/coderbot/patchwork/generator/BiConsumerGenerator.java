package net.coderbot.patchwork.generator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A utility for generating classes that implement java.util.BiConsumer<T, U>
 */
public class BiConsumerGenerator {
	private ClassVisitor visitor;
	private String name;
	private String descriptorA;
	private String descriptorB;
	private String signatureA;
	private String signatureB;

	/**
	 * Creates a new ConsumerGenerator.
	 *
	 * @param visitor The class visitor that will be visited with the generated Consumer class
	 * @param name The name of the class to generate
	 * @param descriptorA The descriptor of the first type parameter (T in Consumer<T>), for example
	 *         "Ljava/lang/String;"
	 * @param descriptorB The descriptor of the second type parameter (T in Consumer<T>), for
	 *         example "Ljava/lang/String;"
	 * @param signatureA The signature of the first type parameter, for example
	 *         "Ljava/util/function/Consumer<Ljava/lang/String;>;"
	 * @param signatureB The signature of the second type parameter, for example
	 *         "Ljava/util/function/Consumer<Ljava/lang/String;>;"
	 */
	public BiConsumerGenerator(ClassVisitor visitor,
			String name,
			String descriptorA,
			String descriptorB,
			String signatureA,
			String signatureB) {
		this.visitor = visitor;
		this.name = name;
		this.descriptorA = descriptorA;
		this.descriptorB = descriptorB;
		this.signatureA = signatureA;
		this.signatureB = signatureB;

		if(signatureA == null) {
			signatureA = descriptorA;
		}

		if(signatureB == null) {
			signatureB = descriptorB;
		}

		visitor.visit(Opcodes.V1_8,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
				name,
				"Ljava/lang/Object;Ljava/util/function/BiConsumer<" + signatureA + signatureB +
						">;",
				"java/lang/Object",
				new String[] { "java/util/function/BiConsumer" });
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
		String signature = null;

		if(this.signatureA != null || this.signatureB != null) {
			signature = "(";

			if(signatureA != null) {
				signature += signatureA;
			} else {
				signature += descriptorA;
			}

			if(signatureB != null) {
				signature += signatureB;
			} else {
				signature += descriptorB;
			}

			signature += ")V";
		}

		return visitor.visitMethod(Opcodes.ACC_PUBLIC,
				"accept",
				"(" + descriptorA + descriptorB + ")V",
				signature,
				null);
	}

	/**
	 * Generates a bridge method and completes the class, calling visitEnd on the class visitor.
	 */
	public void visitEnd() {
		MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE,
				"accept",
				"(Ljava/lang/Object;Ljava/lang/Object;)V",
				null,
				null);

		method.visitVarInsn(Opcodes.ALOAD, 0);

		// CHECKCAST wants a type of the form "java/lang/String" so we strip the L and the ; from
		// the descriptor

		method.visitVarInsn(Opcodes.ALOAD, 1);
		method.visitTypeInsn(Opcodes.CHECKCAST, descriptorA.substring(1, descriptorA.length() - 1));

		method.visitVarInsn(Opcodes.ALOAD, 2);
		method.visitTypeInsn(Opcodes.CHECKCAST, descriptorB.substring(1, descriptorB.length() - 1));

		method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
				name,
				"accept",
				"(" + descriptorA + descriptorB + ")V",
				false);

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(3, 3);
		method.visitEnd();

		visitor.visitEnd();
	}
}
