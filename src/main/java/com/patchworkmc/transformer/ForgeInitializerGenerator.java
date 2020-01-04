package com.patchworkmc.transformer;

import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ForgeInitializerGenerator {
	public static void generate(String className, String modId, Iterable<Map.Entry<String, Consumer<MethodVisitor>>> initializerSteps, ClassVisitor visitor) {
		visitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, "Ljava/lang/Object;Lcom/patchworkmc/api/ForgeInitializer;", "java/lang/Object", new String[] {
				"com/patchworkmc/api/ForgeInitializer" });

		{
			MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			method.visitVarInsn(Opcodes.ALOAD, 0);
			method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			method.visitInsn(Opcodes.RETURN);
			method.visitMaxs(1, 1);
			method.visitEnd();
		}

		{
			MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC, "getModId", "()Ljava/lang/String;", null, null);

			method.visitLdcInsn(modId);
			method.visitInsn(Opcodes.ARETURN);

			method.visitMaxs(1, 1);
			method.visitEnd();
		}

		for (Map.Entry<String, Consumer<MethodVisitor>> step: initializerSteps) {
			String name = step.getKey();
			Consumer<MethodVisitor> generator = step.getValue();

			MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, "()V", null, null);
			generator.accept(method);
		}

		{
			MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC, "onForgeInitialize", "()V", null, null);

			for (Map.Entry<String, Consumer<MethodVisitor>> step: initializerSteps) {
				String name = step.getKey();

				method.visitMethodInsn(Opcodes.INVOKESTATIC, className, name, "()V", false);
			}

			method.visitInsn(Opcodes.RETURN);
			method.visitMaxs(1, 1);
			method.visitEnd();
		}
	}
}
