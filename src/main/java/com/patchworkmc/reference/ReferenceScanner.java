package com.patchworkmc.reference;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ReferenceScanner extends ClassVisitor {
	private Consumer<String> owned;
	private Consumer<String> references;

	public ReferenceScanner(ClassVisitor visitor, Consumer<String> owned, Consumer<String> references) {
		super(Opcodes.ASM7, visitor);

		this.owned = owned;
		this.references = references;
	}

	private String normalize(String type) {
		if (type.startsWith("[")) {
			return descriptorToInternal(stripArrays(type));
		} else {
			return type;
		}
	}

	private String stripArrays(String descriptor) {
		return descriptor.substring(descriptor.lastIndexOf('[') + 1);
	}

	private String descriptorToInternal(String descriptor) {
		int begin = descriptor.indexOf('L') + 1;
		int end = descriptor.indexOf(';');

		if(begin == 0 || end == -1) {
			throw new IllegalArgumentException("invalid descriptor " + descriptor);
		}

		return descriptor.substring(begin, end);
	}

	private boolean isClassDescriptor(String descriptor) {
		return descriptor.startsWith("L") && descriptor.endsWith(";");
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		owned.accept(name);
		references.accept(superName);

		for (String iface: interfaces) {
			references.accept(iface);
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		references.accept(descriptorToInternal(descriptor));

		// TODO: Scan annotation content
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (isClassDescriptor(descriptor)) {
			references.accept(descriptorToInternal(descriptor));
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (exceptions != null) {
			for (String exception: exceptions) {
				references.accept(exception);
			}
		}

		int fromIndex = 0;

		while (fromIndex < descriptor.length()) {
			int begin = descriptor.indexOf('L', fromIndex) + 1;
			int end = descriptor.indexOf(';', fromIndex);

			if (begin == -1 || end == -1 || begin > end) {
				break;
			}

			references.accept(descriptor.substring(begin, end));

			fromIndex = end + 1;
		}

		// TODO: Scan method content
		return new MethodScanner(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class MethodScanner extends MethodVisitor {
		private MethodScanner(MethodVisitor visitor) {
			super(Opcodes.ASM7, visitor);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (isClassDescriptor(normalize(type))) {
				references.accept(normalize(type));
			}

			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			if (isClassDescriptor(normalize(owner))) {
				references.accept(normalize(owner));
			}

			super.visitFieldInsn(opcode, owner, name, descriptor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			references.accept(normalize(owner));
			// TODO: Check Descriptor

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}

		// TODO: InvokeDynamic might have something interesting?


		@Override
		public void visitLdcInsn(Object value) {
			if (value instanceof Type) {
				Type type = (Type) value;

				// TODO: METHOD types

				if (type.getSort() == Type.OBJECT) {
					System.err.println(type.getDescriptor());

					String descriptor = type.getDescriptor();

					if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
						references.accept(descriptorToInternal(descriptor));
					}
				}
			}

			super.visitLdcInsn(value);
		}
	}
}
