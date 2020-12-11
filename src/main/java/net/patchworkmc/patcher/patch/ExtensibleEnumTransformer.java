package net.patchworkmc.patcher.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.transformer.ClassPostTransformer;
import net.patchworkmc.patcher.transformer.VisitorTransformer;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class ExtensibleEnumTransformer extends VisitorTransformer {
	private static final String PATCHWORK_ENUM_HACKS = "net/patchworkmc/api/enumhacks/EnumHacks";

	// enum class -> method name on EnumHacks
	private static final Map<String, String> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1814", "createRarity"); // Rarity
		redirects.put("net/minecraft/class_1311", "createEntityCategory"); // EntityCategory
		redirects.put("net/minecraft/class_3785$class_3786", "createStructurePoolProjection"); // StructurePool.Projection
		redirects.put("net/minecraft/class_3124$class_3125", "createOreFeatureConfigTarget"); // OreFeatureConfig.Target
		redirects.put("net/minecraft/class_2582", "createBannerPattern"); // BannerPattern
		redirects.put("net/minecraft/class_1317$class_1319", "createSpawnRestrictionLocation"); // SpawnRestriction.Location
		redirects.put("net/minecraft/class_1886", "createEnchantmentTarget"); // EnchantmentTarget
	}

	public ExtensibleEnumTransformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer widenings) {
		super(version, jar, parent, widenings);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private static class MethodTransformer extends MethodVisitor {
		private MethodTransformer(MethodVisitor parent) {
			super(Opcodes.ASM9, parent);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			String methodName;

			if (opcode == Opcodes.INVOKESTATIC && name.equals("create") && (methodName = redirects.get(owner)) != null) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, PATCHWORK_ENUM_HACKS, methodName, descriptor, false);
			} else {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}
		}
	}
}
