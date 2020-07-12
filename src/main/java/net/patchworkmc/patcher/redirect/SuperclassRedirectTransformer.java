package net.patchworkmc.patcher.redirect;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SuperclassRedirectTransformer extends ClassVisitor {
	private Map<String, String> redirects;

	public SuperclassRedirectTransformer(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);

		redirects = new HashMap<>();
	}

	public void redirectSuperclass(String from, String to) {
		String existing = redirects.put(from, to);

		if (existing != null) {
			throw new IllegalStateException("Conflicting superclass redirection for " + from + ": already redirected to " + existing + ", attempting to redirect to " + to);
		}
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		superName = redirects.getOrDefault(superName, superName);

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class MethodTransformer extends MethodVisitor {
		private MethodTransformer(MethodVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (opcode == Opcodes.NEW) {
				type = redirects.getOrDefault(type, type);
			}

			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (name.equals("<init>")) {
				owner = redirects.getOrDefault(owner, owner);
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
