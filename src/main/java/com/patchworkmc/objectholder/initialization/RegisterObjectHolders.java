package com.patchworkmc.objectholder.initialization;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.Patchwork;
import com.patchworkmc.objectholder.ObjectHolder;

public class RegisterObjectHolders implements Consumer<MethodVisitor> {
	private static HashMap<String, String> classToRegistry = new HashMap<>();
	private static HashMap<String, String> classToRegistryType = new HashMap<>();

	static {
		addMapping("class_2248", "field_11146", "Lnet/minecraft/class_2348;"); // Block -> BLOCK
		addMapping("class_1959", "field_11153", "Lnet/minecraft/class_2378;"); // Biome -> BIOME
		addMapping("class_1792", "field_11142", "Lnet/minecraft/class_2348;"); // Item -> ITEM
		addMapping("class_3523", "field_11147", "Lnet/minecraft/class_2378;"); // SurfaceBuilder -> SURFACE_BUILDER
		addMapping("class_1865", "field_17598", "Lnet/minecraft/class_2378;"); // RecipeSerializer -> RECIPE_SERIALIZER
	}

	private Iterable<Map.Entry<String, ObjectHolder>> objectHolderEntries;

	public RegisterObjectHolders(Iterable<Map.Entry<String, ObjectHolder>> objectHolderEntries) {
		this.objectHolderEntries = objectHolderEntries;
	}

	private static void addMapping(String clazz, String registry, String registryType) {
		classToRegistry.put("Lnet/minecraft/" + clazz + ";", registry);
		classToRegistryType.put("Lnet/minecraft/" + clazz + ";", registryType);
	}

	@Override
	public void accept(MethodVisitor method) {
		for (Map.Entry<String, ObjectHolder> entry : objectHolderEntries) {
			String shimName = entry.getKey();
			ObjectHolder holder = entry.getValue();

			String registry = classToRegistry.get(holder.getDescriptor());
			String registryType = classToRegistryType.get(holder.getDescriptor());

			String registerDescriptor = "(Lnet/minecraft/class_2378;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V";

			method.visitFieldInsn(Opcodes.GETSTATIC, "com/patchworkmc/api/registries/ObjectHolderRegistry", "INSTANCE", "Lcom/patchworkmc/api/registries/ObjectHolderRegistry;");

			if (registry == null) {
				if (holder.getDescriptor().startsWith("Lnet/minecraft/class_")) {
					Patchwork.LOGGER.error("Dont know what registry the minecraft class " + holder.getDescriptor() + " belongs to, falling back to dynamic!");
				}

				method.visitLdcInsn(Type.getObjectType(holder.getDescriptor().substring(1, holder.getDescriptor().length() - 1)));
				registerDescriptor = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V";
			} else {
				method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/class_2378", // net.minecraft.util.Registry
						registry, registryType);
			}

			method.visitLdcInsn(holder.getNamespace());
			method.visitLdcInsn(holder.getName());
			method.visitTypeInsn(Opcodes.NEW, shimName);
			method.visitInsn(Opcodes.DUP);

			method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);

			method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/patchworkmc/api/registries/ObjectHolderRegistry", "register", registerDescriptor, false);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(6, 0);
		method.visitEnd();
	}
}
