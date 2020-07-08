package com.patchworkmc.redirect;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RedirectTransformer extends ClassVisitor {
	private Map<Target, Target> staticMethodRedirects;

	public RedirectTransformer(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);

		staticMethodRedirects = new HashMap<>();
	}

	public void redirectStaticMethod(Target from, Target to) {
		Target existing = staticMethodRedirects.put(from, to);

		if (existing != null) {
			throw new IllegalStateException("Conflicting static method redirection for " + from + ": already redirected to " + existing + ", attempting to redirect to " + to);
		}
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
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			Target from = new Target(owner, name);

			if (opcode == Opcodes.INVOKESTATIC) {
				Target to = staticMethodRedirects.get(from);

				if (to != null) {
					super.visitMethodInsn(Opcodes.INVOKESTATIC, to.getOwner(), to.getName(), descriptor, false);

					return;
				}
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
