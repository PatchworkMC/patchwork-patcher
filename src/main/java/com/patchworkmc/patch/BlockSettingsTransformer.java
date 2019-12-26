package com.patchworkmc.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BlockSettingsTransformer extends ClassVisitor {
	// The intermediary name for Block$Settings
	private static final String BLOCK_SETTINGS = "net/minecraft/class_2248$class_2251";

	private static final String BLOCK_SETTINGS_DESC = "Lnet/minecraft/class_2248$class_2251;";

	// Patchwork's shim to call the protected methods using FabricBlockSettings
	private static final String PATCHWORK_BLOCK_SETTINGS = "net/coderbot/patchwork/block/PatchworkBlockSettings";

	private static final Map<String, String> redirects = new HashMap<>();

	static {
		redirects.put("method_16229", "dropsNothing");
		redirects.put("method_9618", "breakInstantly");
		redirects.put("method_9624", "hasDynamicBounds");
		redirects.put("method_9626", "sounds");
		redirects.put("method_9631", "lightLevel");
		redirects.put("method_9632", "strength"); // with one argument
		redirects.put("method_9640", "ticksRandomly");
	}

	public BlockSettingsTransformer(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);
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
			if (!owner.equals(BLOCK_SETTINGS)) {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

				return;
			}

			String redirect = redirects.get(name);

			if (redirect == null) {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

				return;
			}

			String staticDescriptor = "(" + BLOCK_SETTINGS_DESC + descriptor.substring(1);

			super.visitMethodInsn(Opcodes.INVOKESTATIC, PATCHWORK_BLOCK_SETTINGS, redirect, staticDescriptor, false);
		}
	}
}
