package net.coderbot.patchwork.objectholder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ForgeInitializerGenerator {
	private static HashMap<String, String> classToRegistry = new HashMap<>();
	private static HashMap<String, String> classToRegistryType = new HashMap<>();

	static {
		addMapping("class_2248", "field_11146", "Lnet/minecraft/class_2348;"); // Block -> BLOCK
		addMapping("class_1959", "field_11153", "Lnet/minecraft/class_2378;"); // Biome -> BIOME
		addMapping("class_3523",
				"field_11147",
				"Lnet/minecraft/class_2378;"); // SurfaceBuilder -> SURFACE_BUILDER
	}

	private static void addMapping(String clazz, String registry, String registryType) {
		classToRegistry.put("Lnet/minecraft/" + clazz + ";", registry);
		classToRegistryType.put("Lnet/minecraft/" + clazz + ";", registryType);
	}

	public static void generate(String className,
			List<Map.Entry<String, ObjectHolder>> entries,
			ClassVisitor visitor) {

		visitor.visit(Opcodes.V1_8,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
				className,
				"Ljava/lang/Object;Lnet/coderbot/patchwork/ForgeInitializer;",
				"java/lang/Object",
				new String[] { "net/coderbot/patchwork/ForgeInitializer" });

		{
			MethodVisitor method =
					visitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			method.visitVarInsn(Opcodes.ALOAD, 0);
			method.visitMethodInsn(
					Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			method.visitInsn(Opcodes.RETURN);
			method.visitMaxs(1, 1);
			method.visitEnd();
		}

		{
			MethodVisitor method =
					visitor.visitMethod(Opcodes.ACC_PUBLIC, "onForgeInitialize", "()V", null, null);

			for(Map.Entry<String, ObjectHolder> entry : entries) {
				String shimName = entry.getKey();
				ObjectHolder holder = entry.getValue();

				String registry = classToRegistry.get(holder.getDescriptor());
				String registryType = classToRegistryType.get(holder.getDescriptor());

				if(registry == null) {
					throw new IllegalArgumentException(
							"Missing a mapping for " + holder.getDescriptor());
				}

				method.visitFieldInsn(Opcodes.GETSTATIC,
						"net/coderbot/patchwork/ObjectHolderRegistry",
						"INSTANCE",
						"Lnet/coderbot/patchwork/ObjectHolderRegistry;");

				method.visitFieldInsn(Opcodes.GETSTATIC,
						"net/minecraft/class_2378", // net.minecraft.util.Registry
						registry,
						registryType);

				method.visitLdcInsn(holder.getNamespace());
				method.visitLdcInsn(holder.getName());
				method.visitTypeInsn(Opcodes.NEW, shimName);
				method.visitInsn(Opcodes.DUP);

				method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);

				method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
						"net/coderbot/patchwork/ObjectHolderRegistry",
						"register",
						"(Lnet/minecraft/class_2378;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V",
						false);
			}

			method.visitInsn(Opcodes.RETURN);

			method.visitMaxs(6, 1);
			method.visitEnd();
		}
	}
}
