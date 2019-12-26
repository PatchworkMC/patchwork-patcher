package com.patchworkmc.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ItemGroupTransformer extends ClassVisitor {
	// The intermediary name for ItemGroup
	private static final String ITEM_GROUP = "net/minecraft/class_1761";

	// Patchwork's replacement item group classs
	private static final String PATCHWORK_ITEM_GROUP = "com/patchworkmc/itemgroup/PatchworkItemGroup";

	private boolean applies;

	public ItemGroupTransformer(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);

		applies = false;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (superName.equals(ITEM_GROUP)) {
			superName = PATCHWORK_ITEM_GROUP;
			applies = true;
		}

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (applies && name.equals("<init>")) {
			System.out.println("Patching an ItemGroup to use Patchwork");

			return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	private static class MethodTransformer extends MethodVisitor {
		private MethodTransformer(MethodVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode != Opcodes.INVOKESPECIAL) {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}

			System.out.println("Found InvokeSpecial in <init> " + owner + " " + name + " " + descriptor + " " + isInterface);

			if (!owner.equals(ITEM_GROUP) || !name.equals("<init>")) {
				return;
			}

			if (!descriptor.equals("(Ljava/lang/String;)V")) {
				System.err.println("Unexpected descriptor for super() in ItemGroup: " + descriptor);
			}

			super.visitMethodInsn(Opcodes.INVOKESPECIAL, PATCHWORK_ITEM_GROUP, name, descriptor, isInterface);
		}
	}
}
