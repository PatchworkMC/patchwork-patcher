package net.patchworkmc.patcher.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.transformer.api.Transformer;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class BiomeLayersTransformer extends Transformer {
	private static final String BIOME_LAYERS_NAME = "net/minecraft/class_3645";
	private static final String PATCHWORK_BIOME_LAYERS_NAME = "net/patchworkmc/api/levelgenerators/PatchworkBiomeLayers";

	public BiomeLayersTransformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent) {
		super(version, jar, parent);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private static class MethodTransformer extends MethodVisitor {
		MethodTransformer(MethodVisitor methodVisitor) {
			super(Opcodes.ASM7, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKESTATIC && BIOME_LAYERS_NAME.equals(owner) && "getModdedBiomeSize".equals(name)) {
				owner = PATCHWORK_BIOME_LAYERS_NAME;
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
