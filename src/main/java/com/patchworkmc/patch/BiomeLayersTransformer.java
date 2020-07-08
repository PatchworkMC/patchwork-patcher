package com.patchworkmc.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BiomeLayersTransformer extends ClassVisitor {
	private static final String BIOME_LAYERS_NAME = "net/minecraft/class_3645";
	private static final String PATCHWORK_BIOME_LAYERS_NAME = "net/patchworkmc/api/levelgenerators/PatchworkBiomeLayers";

	public BiomeLayersTransformer(ClassVisitor classVisitor) {
		super(Opcodes.ASM7, classVisitor);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, BIOME_LAYERS_NAME.equals(superName) ? PATCHWORK_BIOME_LAYERS_NAME : superName, interfaces);
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
		public void visitTypeInsn(int opcode, String type) {
			if (opcode == Opcodes.NEW && BIOME_LAYERS_NAME.equals(type)) {
				type = PATCHWORK_BIOME_LAYERS_NAME;
			}

			super.visitTypeInsn(opcode, type);
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
