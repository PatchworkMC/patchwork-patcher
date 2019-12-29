package com.patchworkmc.objectholder;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.Patchwork;
import com.patchworkmc.event.EventBusSubscriber;

public class ForgeInitializerGenerator {
	private static HashMap<String, String> classToRegistry = new HashMap<>();
	private static HashMap<String, String> classToRegistryType = new HashMap<>();

	static {
		addMapping("class_2248", "field_11146", "Lnet/minecraft/class_2348;"); // Block -> BLOCK
		addMapping("class_1959", "field_11153", "Lnet/minecraft/class_2378;"); // Biome -> BIOME
		addMapping("class_1792", "field_11142", "Lnet/minecraft/class_2348;"); // Item -> ITEM
		addMapping("class_3523", "field_11147", "Lnet/minecraft/class_2378;"); // SurfaceBuilder -> SURFACE_BUILDER
		addMapping("class_1865", "field_17598", "Lnet/minecraft/class_2378;"); // RecipeSerializer -> RECIPE_SERIALIZER
	}

	private static void addMapping(String clazz, String registry, String registryType) {
		classToRegistry.put("Lnet/minecraft/" + clazz + ";", registry);
		classToRegistryType.put("Lnet/minecraft/" + clazz + ";", registryType);
	}

	public static void generate(String modName, String className, String modId, Iterable<Map.Entry<String, String>> staticEventRegistrars, Iterable<Map.Entry<String, String>> instanceEventRegistrars, Iterable<Map.Entry<String, EventBusSubscriber>> subscribers, Iterable<Map.Entry<String, ObjectHolder>> objectHolderEntries, ClassVisitor visitor) {
		visitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, "Ljava/lang/Object;Lcom/patchworkmc/api/ForgeInitializer;", "java/lang/Object", new String[] {"com/patchworkmc/api/ForgeInitializer"});

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

		{
			MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC, "onForgeInitialize", "()V", null, null);

			// TODO: Need to check if the base classes are annotated with @OnlyIn / @Environment

			for (Map.Entry<String, String> entry : staticEventRegistrars) {
				// max stack 4, max locals 1

				String shimName = entry.getKey();
				String baseName = entry.getValue();

				method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "INSTANCE", "Lnet/minecraftforge/eventbus/api/EventRegistrarRegistry;");

				// Remove the starting /
				method.visitLdcInsn(Type.getObjectType(baseName));

				method.visitTypeInsn(Opcodes.NEW, shimName);
				method.visitInsn(Opcodes.DUP);

				method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);
				method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "registerStatic", "(Ljava/lang/Class;Ljava/util/function/Consumer;)V", true);
			}

			for (Map.Entry<String, String> entry : instanceEventRegistrars) {
				// max stack 4, max locals 1

				String shimName = entry.getKey();
				String baseName = entry.getValue();

				method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "INSTANCE", "Lnet/minecraftforge/eventbus/api/EventRegistrarRegistry;");

				// Remove the starting /
				method.visitLdcInsn(Type.getObjectType(baseName));

				method.visitTypeInsn(Opcodes.NEW, shimName);
				method.visitInsn(Opcodes.DUP);

				method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);
				method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/EventRegistrarRegistry", "registerInstance", "(Ljava/lang/Class;Ljava/util/function/BiConsumer;)V", true);
			}

			// Call <init> on the mod class in case it has important initialization functions
			// TODO: This should probably be first...

			method.visitTypeInsn(Opcodes.NEW, modName);
			method.visitMethodInsn(Opcodes.INVOKESPECIAL, modName, "<init>", "()V", false);

			for (Map.Entry<String, EventBusSubscriber> entry : subscribers) {
				// max stack 4, max locals 1?

				String baseName = entry.getKey();
				EventBusSubscriber subscriber = entry.getValue();

				// TODO: Check targetModId

				if (!subscriber.isClient() || !subscriber.isServer()) {
					if (System.getProperty("patchwork:ignore_sided_annotations", "false").equals("true")) {
						Patchwork.LOGGER.warn("Sided @EventBusSubscriber annotations are not supported yet, applying " + subscriber + " from " + baseName + " without sides.");
					} else {
						Patchwork.LOGGER.error("Sided @EventBusSubscriber annotations are not supported yet, skipping: " + subscriber + " attached to: " + baseName);
						continue;
					}
				}

				if (subscriber.getBus() == EventBusSubscriber.Bus.MOD) {
					method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", "get", "()Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;", false);

					method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", "getModEventBus", "()Lnet/minecraftforge/eventbus/api/IEventBus;", false);
				} else {
					method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/common/MinecraftForge", "EVENT_BUS", "Lnet/minecraftforge/eventbus/api/IEventBus;");
				}

				// Remove the starting /
				method.visitLdcInsn(Type.getObjectType(baseName));

				method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/IEventBus", "register", "(Ljava/lang/Object;)V", true);
			}

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

			method.visitMaxs(6, 1);
			method.visitEnd();
		}
	}
}
