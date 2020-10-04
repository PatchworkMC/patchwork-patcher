package net.patchworkmc.patcher.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.transformer.api.ClassPostTransformer;
import net.patchworkmc.patcher.transformer.api.Transformer;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class KeyBindingsTransformer extends Transformer {
	// The intermediary name for KeyBinding
	private static final String KEY_BINDING = "net/minecraft/class_304";

	// Patchwork's replacement key binding class
	private static final String PATCHWORK_KEY_BINDING = "net/patchworkmc/api/keybindings/PatchworkKeyBinding";

	public KeyBindingsTransformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer widenings) {
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
			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>") && owner.equals(KEY_BINDING)) {
				super.visitMethodInsn(opcode, PATCHWORK_KEY_BINDING, name, descriptor, isInterface);
			} else {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (type.equals(KEY_BINDING) && opcode == Opcodes.NEW) {
				super.visitTypeInsn(opcode, PATCHWORK_KEY_BINDING);
			} else {
				super.visitTypeInsn(opcode, type);
			}
		}
	}
}
