package net.patchworkmc.patcher.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.transformer.api.Transformer;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class LevelGeneratorTypeTransformer extends Transformer {
	private static final String LEVEL_GENERATOR_TYPE_NAME = "net/minecraft/class_1942";
	private static final String PATCHWORK_LEVEL_GENERATOR_TYPE_NAME = "net/patchworkmc/api/levelgenerators/PatchworkLevelGeneratorType";

	public LevelGeneratorTypeTransformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent) {
		super(version, jar, parent);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, LEVEL_GENERATOR_TYPE_NAME.equals(superName) ? PATCHWORK_LEVEL_GENERATOR_TYPE_NAME : superName, interfaces);
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
			if (opcode == Opcodes.NEW && LEVEL_GENERATOR_TYPE_NAME.equals(type)) {
				type = PATCHWORK_LEVEL_GENERATOR_TYPE_NAME;
			}

			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKESPECIAL && LEVEL_GENERATOR_TYPE_NAME.equals(owner) && "<init>".equals(name) && "(Ljava/lang/String;)V".equals(descriptor)) {
				owner = PATCHWORK_LEVEL_GENERATOR_TYPE_NAME;
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
