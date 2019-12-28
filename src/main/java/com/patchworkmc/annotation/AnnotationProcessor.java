package com.patchworkmc.annotation;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.patchworkmc.Patchwork;

public class AnnotationProcessor extends ClassVisitor {
	private Consumer<String> consumer;

	public AnnotationProcessor(ClassVisitor parent, Consumer<String> consumer) {
		super(Opcodes.ASM7, parent);

		this.consumer = consumer;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals("Lnet/minecraftforge/fml/common/Mod;")) {
			return new StringAnnotationHandler(consumer);
		} else if (descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
			return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
		} else if (descriptor.equals("Lmcp/MethodsReturnNonnullByDefault;")) {
			// TODO: Rewrite this annotation to something standardized

			Patchwork.LOGGER.error("Stripping class annotation Lmcp/MethodsReturnNonnullByDefault; as it is not supported yet");

			return null;
		} else if (descriptor.equals("Lscala/reflect/ScalaSignature;")) {
			// return new StringAnnotationHandler("bytes", new ScalaSignatureHandler());
			// Ignore scala signatures for now

			return super.visitAnnotation(descriptor, visible);
		} else if (descriptor.startsWith("Ljava")) {
			// Java annotations are ignored

			return super.visitAnnotation(descriptor, visible);
		} else {
			Patchwork.LOGGER.error("Unknown class annotation: " + descriptor + " " + visible);
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return new FieldScanner(super.visitField(access, name, descriptor, signature, value), name, descriptor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodScanner(super.visitMethod(access, name, descriptor, signature, exceptions), name, descriptor, signature);
	}

	static class FieldScanner extends FieldVisitor {
		String name;

		FieldScanner(FieldVisitor parent, String name, String descriptor) {
			super(Opcodes.ASM7, parent);

			this.name = name;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
				return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
			} else if (descriptor.startsWith("Ljava")) {
				// Java annotations are ignored

				return super.visitAnnotation(descriptor, visible);
			}

			Patchwork.LOGGER.error("Unknown field annotation: " + descriptor + " " + visible);
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
			if (descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
				return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
			} else if (descriptor.startsWith("Ljava")) {
				// Java annotations are ignored

				return super.visitAnnotation(descriptor, visible);
			} else {
				Patchwork.LOGGER.error("Unknown method annotation: " + descriptor + " " + visible);
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

			Patchwork.LOGGER.error("    %s -> %s", name, value);
		}
	}
}
