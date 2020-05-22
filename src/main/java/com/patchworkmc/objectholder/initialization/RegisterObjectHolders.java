package com.patchworkmc.objectholder.initialization;

import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.Patchwork;
import com.patchworkmc.objectholder.ObjectHolder;

public class RegisterObjectHolders implements Consumer<MethodVisitor> {
	// net.minecraft.util.Registry
	private static final String REGISTRY = "net/minecraft/class_2378";

	private Iterable<Map.Entry<String, ObjectHolder>> objectHolderEntries;

	public RegisterObjectHolders(Iterable<Map.Entry<String, ObjectHolder>> objectHolderEntries) {
		this.objectHolderEntries = objectHolderEntries;
	}

	@Override
	public void accept(MethodVisitor method) {
		for (Map.Entry<String, ObjectHolder> entry : objectHolderEntries) {
			String shimName = entry.getKey();
			ObjectHolder holder = entry.getValue();

			VanillaRegistry registry = VanillaRegistry.get(holder.getDescriptor());

			String registerDescriptor = "(Lnet/minecraft/class_2378;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V";

			method.visitFieldInsn(Opcodes.GETSTATIC, "net/patchworkmc/api/registries/ObjectHolderRegistry", "INSTANCE", "Lnet/patchworkmc/api/registries/ObjectHolderRegistry;");

			if (registry == null) {
				if (holder.getDescriptor().startsWith("Lnet/minecraft/class_")) {
					Patchwork.LOGGER.warn("Don't know what registry the minecraft class " + holder.getDescriptor() + " belongs to, falling back to dynamic!");
				}

				method.visitLdcInsn(Type.getObjectType(holder.getDescriptor().substring(1, holder.getDescriptor().length() - 1)));
				registerDescriptor = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V";
			} else {
				method.visitFieldInsn(Opcodes.GETSTATIC, REGISTRY, registry.getField(), registry.getFieldDescriptor());
			}

			method.visitLdcInsn(holder.getNamespace());
			method.visitLdcInsn(holder.getName());
			method.visitTypeInsn(Opcodes.NEW, shimName);
			method.visitInsn(Opcodes.DUP);

			method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);

			method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/patchworkmc/api/registries/ObjectHolderRegistry", "register", registerDescriptor, false);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(6, 0);
		method.visitEnd();
	}
}
