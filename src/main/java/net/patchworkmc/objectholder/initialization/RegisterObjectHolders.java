package net.patchworkmc.objectholder.initialization;

import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RegisterObjectHolders implements Consumer<MethodVisitor> {
	private Iterable<String> objectHolderClasses;

	public RegisterObjectHolders(Iterable<String> objectHolderClasses) {
		this.objectHolderClasses = objectHolderClasses;
	}

	@Override
	public void accept(MethodVisitor method) {
		for (String owner : objectHolderClasses) {
			method.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "patchwork$registerObjectHolders", "()V", false);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(0, 0);
		method.visitEnd();
	}
}
