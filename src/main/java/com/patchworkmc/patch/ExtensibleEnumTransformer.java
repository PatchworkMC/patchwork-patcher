package com.patchworkmc.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ExtensibleEnumTransformer extends ClassVisitor {
	private static final String PATCHWORK_ENUM_HACKS = "com/patchworkmc/api/enumhacks/EnumHacks";

	//enum class -> method name on EnumHacks
	private static final Map<String, String> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1814", "createRarity"); //Rarity
		redirects.put("net/minecraft/class_1311", "createEntityCategory"); //EntityCategory
		redirects.put("net/minecraft/class_3785$class_3786", "createStructurePoolProjection"); //StructurePool.Projection
		redirects.put("net/minecraft/class_3124$class_3125", "createOreFeatureConfigTarget"); //OreFeatureConfig.Target
		redirects.put("net/minecraft/class_2582", "createBannerPattern"); //BannerPattern
		redirects.put("net/minecraft/class_1317$class_1319", "createSpawnRestrictionLocation"); //SpawnRestriction.Location
		redirects.put("net/minecraft/class_1886", "createEnchantmentTarget"); //EnchantmentTarget
	}

	public ExtensibleEnumTransformer(ClassVisitor classVisitor) {
		super(Opcodes.ASM7, classVisitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private static class MethodTransformer extends MethodVisitor {
		private MethodTransformer(MethodVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKESTATIC && name.equals("create") && redirects.containsKey(owner)) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, PATCHWORK_ENUM_HACKS, redirects.get(owner), descriptor, false);
				return;
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
