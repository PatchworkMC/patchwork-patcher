package net.coderbot.patchwork.annotation;

import java.util.function.Consumer;

import org.objectweb.asm.*;

public class AnnotationProcessor extends ClassVisitor {
	private Consumer<String> consumer;

	public AnnotationProcessor(ClassVisitor parent, Consumer<String> consumer) {
		super(Opcodes.ASM7, parent);

		this.consumer = consumer;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if(descriptor.equals("Lnet/minecraftforge/fml/common/Mod;")) {
			return new StringAnnotationHandler(consumer);
		} else {
			System.err.println("Unknown class annotation: " + descriptor + " " + visible);
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	@Override
	public FieldVisitor visitField(int access,
			String name,
			String descriptor,
			String signature,
			Object value) {
		return new FieldScanner(
				super.visitField(access, name, descriptor, signature, value), name, descriptor);
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {
		return new MethodScanner(super.visitMethod(access, name, descriptor, signature, exceptions),
				name,
				descriptor,
				signature);
	}

	static class FieldScanner extends FieldVisitor {
		String name;

		FieldScanner(FieldVisitor parent, String name, String descriptor) {
			super(Opcodes.ASM7, parent);

			this.name = name;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			System.err.println("Unknown field annotation: " + descriptor + " " + visible);
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	static class MethodScanner extends MethodVisitor {
		String name;
		String descriptor;
		String signature;

		MethodScanner(MethodVisitor parent, String name, String descriptor, String signature) {
			super(Opcodes.ASM7, parent);

			this.name = name;
			this.descriptor = descriptor;
			this.signature = signature;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if(descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
				return new OnlyInRewriter(
						super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
			} else {
				System.err.println("Unknown method annotation: " + descriptor + " " + visible);
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
			}
		}
	}

	static class AnnotationPrinter extends AnnotationVisitor {
		AnnotationPrinter(AnnotationVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);

			System.err.println("    " + name + "->" + value);
		}
	}
}
