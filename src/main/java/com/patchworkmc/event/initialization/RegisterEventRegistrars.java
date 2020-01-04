package com.patchworkmc.event.initialization;

import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class RegisterEventRegistrars implements Consumer<MethodVisitor> {
	private Iterable<Map.Entry<String, String>> staticEventRegistrars;
	private Iterable<Map.Entry<String, String>> instanceEventRegistrars;

	public RegisterEventRegistrars(Iterable<Map.Entry<String, String>> staticEventRegistrars, Iterable<Map.Entry<String, String>> instanceEventRegistrars) {
		this.staticEventRegistrars = staticEventRegistrars;
		this.instanceEventRegistrars = instanceEventRegistrars;
	}

	@Override
	public void accept(MethodVisitor method) {
		for (Map.Entry<String, String> entry : staticEventRegistrars) {
			String shimName = entry.getKey();
			String baseName = entry.getValue();

			method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "INSTANCE", "Lnet/minecraftforge/eventbus/api/EventRegistrarRegistry;");

			method.visitLdcInsn(Type.getObjectType(baseName));

			method.visitTypeInsn(Opcodes.NEW, shimName);
			method.visitInsn(Opcodes.DUP);

			method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);
			method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "registerStatic", "(Ljava/lang/Class;Ljava/util/function/Consumer;)V", true);
		}

		for (Map.Entry<String, String> entry : instanceEventRegistrars) {
			String shimName = entry.getKey();
			String baseName = entry.getValue();

			method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "INSTANCE", "Lnet/minecraftforge/eventbus/api/EventRegistrarRegistry;");

			method.visitLdcInsn(Type.getObjectType(baseName));

			method.visitTypeInsn(Opcodes.NEW, shimName);
			method.visitInsn(Opcodes.DUP);

			method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);
			method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "registerInstance", "(Ljava/lang/Class;Ljava/util/function/BiConsumer;)V", true);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(4, 0);
		method.visitEnd();
	}
}
