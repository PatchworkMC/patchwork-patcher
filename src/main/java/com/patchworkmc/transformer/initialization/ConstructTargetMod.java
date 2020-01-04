package com.patchworkmc.transformer.initialization;

import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Invokes the constructor of the mod class in case it has important initialization functions.
 */
public class ConstructTargetMod implements Consumer<MethodVisitor> {
	private String modName;

	public ConstructTargetMod(String modName) {
		this.modName = modName;
	}

	@Override
	public void accept(MethodVisitor method) {
		method.visitTypeInsn(Opcodes.NEW, modName);
		method.visitMethodInsn(Opcodes.INVOKESPECIAL, modName, "<init>", "()V", false);
		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(1, 0);
		method.visitEnd();
	}
}
