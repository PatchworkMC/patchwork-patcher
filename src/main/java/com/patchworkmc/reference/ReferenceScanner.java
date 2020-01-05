package com.patchworkmc.reference;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReferenceScanner extends ClassVisitor {
	private Consumer<String> owned;
	private Consumer<String> references;

	public ReferenceScanner(ClassVisitor visitor, Consumer<String> owned, Consumer<String> references) {
		super(Opcodes.ASM7, visitor);

		this.owned = owned;
		this.references = references;
	}

	private String descriptorToInternal(String descriptor) {
		return descriptor.substring(1, descriptor.length() - 1);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		owned.accept(name);
		references.accept(superName);

		for(String iface: interfaces) {
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
		if(descriptor.startsWith("L") && descriptor.endsWith(";")) {
			references.accept(descriptorToInternal(descriptor));
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (exceptions != null) {
			for(String exception: exceptions) {
				references.accept(exception);
			}
		}

		int fromIndex = 0;

		while (fromIndex < descriptor.length()) {
			int begin = descriptor.indexOf('L', fromIndex) + 1;
			int end = descriptor.indexOf(';', fromIndex);

			if(begin == -1 || end == -1 || begin > end) {
				break;
			}

			references.accept(descriptor.substring(begin, end));

			fromIndex = end + 1;
		}

		// TODO: Scan method content
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
