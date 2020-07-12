package net.patchworkmc.patcher.redirect;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodRedirectTransformer extends ClassVisitor {
	private Map<Target, Target> staticMethodRedirects;
	private Map<Target, InstanceMethodRedirect> instanceMethodRedirects;

	public MethodRedirectTransformer(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);

		staticMethodRedirects = new HashMap<>();
		instanceMethodRedirects = new HashMap<>();
	}

	public void redirectStaticMethod(Target from, Target to) {
		Target existing = staticMethodRedirects.put(from, to);

		if (existing != null) {
			throw new IllegalStateException("Conflicting static method redirection for " + from + ": already redirected to " + existing + ", attempting to redirect to " + to);
		}
	}

	public void redirectInstanceMethod(Target from, Target to) {
		redirectInstance(from, new InstanceMethodRedirect(to, false));
	}

	public void redirectInstanceMethodToStatic(Target from, Target to) {
		redirectInstance(from, new InstanceMethodRedirect(to, true));
	}

	private void redirectInstance(Target from, InstanceMethodRedirect redirect) {
		InstanceMethodRedirect existing = instanceMethodRedirects.put(from, redirect);

		if (existing != null) {
			throw new IllegalStateException("Conflicting instance method redirection for " + from + ": already redirected to " + existing.target + ", attempting to redirect to " + redirect.target);
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
					owner = to.getOwner();
					name = to.getName();
				}
			} else if (opcode == Opcodes.INVOKEVIRTUAL) {
				InstanceMethodRedirect redirect = instanceMethodRedirects.get(from);

				if (redirect != null) {
					owner = redirect.target.getOwner();
					name = redirect.target.getName();

					if (redirect.toStatic) {
						opcode = Opcodes.INVOKESTATIC;
						descriptor = "(L" + owner + ";" + descriptor.substring(1);
					}
				}
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}

	private static class InstanceMethodRedirect {
		final Target target;
		final boolean toStatic;

		InstanceMethodRedirect(Target target, boolean toStatic) {
			this.target = target;
			this.toStatic = toStatic;
		}
	}
}
