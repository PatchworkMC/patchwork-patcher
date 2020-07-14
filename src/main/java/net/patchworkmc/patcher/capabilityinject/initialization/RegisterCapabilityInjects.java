package net.patchworkmc.patcher.capabilityinject.initialization;

import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RegisterCapabilityInjects implements Consumer<MethodVisitor> {
	private Iterable<String> objectHolderClasses;

	public RegisterCapabilityInjects(Iterable<String> objectHolderClasses) {
		this.objectHolderClasses = objectHolderClasses;
	}

	@Override
	public void accept(MethodVisitor method) {
		for (String owner : objectHolderClasses) {
			method.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "patchwork$registerCapabilityInjects", "()V", false);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(0, 0);
		method.visitEnd();
	}
}
