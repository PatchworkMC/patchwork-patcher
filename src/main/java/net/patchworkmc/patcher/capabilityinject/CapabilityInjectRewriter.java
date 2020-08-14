package net.patchworkmc.patcher.capabilityinject;

import static org.objectweb.asm.Opcodes.ASM7;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.annotation.StringAnnotationHandler;
import net.patchworkmc.patcher.util.LambdaVisitors;

public class CapabilityInjectRewriter extends ClassVisitor {
	public static final String CAPABILITY_DESC = "(Lnet/minecraftforge/common/capabilities/Capability;)V";
	public static final String PREFIX = "patchwork$capabilityInject$";

	private String className;
	private List<CapabilityInject> injects = new ArrayList<>();

	public CapabilityInjectRewriter(ClassVisitor classVisitor) {
		super(ASM7, classVisitor);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}

	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor parent = super.visitField(access, name, descriptor, signature, value);
		return new FieldScanner(parent, access, name, descriptor);
	}

	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals("patchwork$registerCapabilityInjects") || name.startsWith(PREFIX)) {
			throw new IllegalArgumentException("Class already contained a method named " + name + ", this name is reserved by Patchwork!");
		}

		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodScanner(parent, access, name, descriptor);
	}

	public void visitEnd() {
		this.generateInjectHandlers();

		if (!this.injects.isEmpty()) {
			MethodVisitor register = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "patchwork$registerCapabilityInjects", "()V", null, null);

			if (register != null) {
				this.generateInjectRegistrations(register);
			}
		}

		super.visitEnd();
	}

	private void generateInjectRegistrations(MethodVisitor method) {
		method.visitCode();

		for (CapabilityInject inject : this.injects) {
			// Put the event on the stack (1)
			method.visitLdcInsn(inject.getType().getClassName());
			method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/patchworkmc/api/capability/CapabilityRegisteredCallback", "event", "(Ljava/lang/String;)Lnet/fabricmc/fabric/api/event/Event;", true);

			// Put our handler on the stack (2)
			Handle handle;

			if (inject.isMethod()) {
				handle = new Handle(Opcodes.H_INVOKESTATIC, this.className, inject.getName(), CAPABILITY_DESC, false);
			} else {
				handle = new Handle(Opcodes.H_INVOKESTATIC, this.className, PREFIX + inject.getName(), CAPABILITY_DESC, false);
			}

			method.visitInvokeDynamicInsn("onCapabilityRegistered", "()Lnet/patchworkmc/api/capability/CapabilityRegisteredCallback;", LambdaVisitors.METAFACTORY, Type.getMethodType(CAPABILITY_DESC), handle, Type.getMethodType(CAPABILITY_DESC));

			// Register the event (0)
			method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/fabricmc/fabric/api/event/Event", "register", "(Ljava/lang/Object;)V", false);
		}

		method.visitInsn(Opcodes.RETURN);
		method.visitMaxs(2, 0);
		method.visitEnd();
	}

	private void generateInjectHandlers() {
		for (CapabilityInject inject : this.injects) {
			String descriptor = "(Lnet/minecraftforge/common/capabilities/Capability;)V";
			String name = PREFIX + inject.getName();

			if (!inject.isMethod()) {
				MethodVisitor handler = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, descriptor, null, null);

				if (handler != null) {
					handler.visitCode();

					// Get the capability (1)
					handler.visitVarInsn(Opcodes.ALOAD, 0);

					// Handle the capability (0)
					handler.visitFieldInsn(Opcodes.PUTSTATIC, this.className, inject.getName(), "Lnet/minecraftforge/common/capabilities/Capability;");

					handler.visitInsn(Opcodes.RETURN);
					handler.visitMaxs(1, 1);
					handler.visitEnd();
				}
			}
		}
	}

	public List<CapabilityInject> getInjects() {
		return this.injects;
	}

	private class MethodScanner extends MethodVisitor {
		private final int access;
		private final String name;

		MethodScanner(MethodVisitor parent, int access, String name, String descriptor) {
			super(ASM7, parent);
			this.access = access;
			this.name = name;
		}

		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.equals("Lnet/minecraftforge/common/capabilities/CapabilityInject;")) {
				if ((this.access & Opcodes.ACC_STATIC) == 0) {
					Patchwork.LOGGER.error("Method " + this.name + " marked with an @CapabilityInject annotation was not static! All @CapabilityInject methods must be static.");
					return null;
				} else {
					return new StringAnnotationHandler(
							typeStr -> injects.add(new CapabilityInject(this.name, Type.getType(typeStr), true)));
				}
			} else {
				return super.visitAnnotation(descriptor, visible);
			}
		}
	}

	private class FieldScanner extends FieldVisitor {
		private final int access;
		private final String name;

		FieldScanner(FieldVisitor parent, int access, String name, String descriptor) {
			super(ASM7, parent);
			this.access = access;
			this.name = name;
		}

		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.equals("Lnet/minecraftforge/common/capabilities/CapabilityInject;")) {
				if ((this.access & Opcodes.ACC_STATIC) == 0) {
					Patchwork.LOGGER.error("Field " + this.name + " marked with an @CapabilityInject annotation was not static! All @CapabilityInject fields must be static.");
					return null;
				} else {
					return new StringAnnotationHandler(
							typeStr -> injects.add(new CapabilityInject(this.name, Type.getType(typeStr), false)));
				}
			} else {
				return super.visitAnnotation(descriptor, visible);
			}
		}
	}
}
