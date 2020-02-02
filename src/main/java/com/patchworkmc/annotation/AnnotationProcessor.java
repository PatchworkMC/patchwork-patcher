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

	private static boolean isKotlinMetadata(String descriptor) {
		// TODO: This is specific to one mod
		return descriptor.startsWith("Lcom/greenapple/glacia/embedded/kotlin/");
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals("Lnet/minecraftforge/fml/common/Mod;")) {
			return new StringAnnotationHandler(consumer);
		} else if (descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
			return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
		} else if (descriptor.equals("Lmcp/MethodsReturnNonnullByDefault;")) {
			// TODO: Rewrite this annotation to something standardized

			Patchwork.LOGGER.warn("Stripping class annotation Lmcp/MethodsReturnNonnullByDefault; as it is not supported yet");

			return null;
		} else if (descriptor.equals("Lscala/reflect/ScalaSignature;")) {
			// return new StringAnnotationHandler("bytes", new ScalaSignatureHandler());
			// Ignore scala signatures for now

			return super.visitAnnotation(descriptor, visible);
		} else if (descriptor.startsWith("Ljava")) {
			// Java annotations are ignored

			return super.visitAnnotation(descriptor, visible);
		} else if (isKotlinMetadata(descriptor)) {
			// Ignore Kotlin metadata

			return super.visitAnnotation(descriptor, visible);
		} else {
			Patchwork.LOGGER.warn("Unknown class annotation: " + descriptor + " " + visible);
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return new FieldScanner(super.visitField(access, name, descriptor, signature, value));
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodScanner(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	static class FieldScanner extends FieldVisitor {
		FieldScanner(FieldVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
				return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
			} else if (descriptor.equals("Lorg/jetbrains/annotations/NotNull;") || descriptor.equals("Lorg/jetbrains/annotations/Nullable;")) {
				// Ignore @NotNull / @Nullable annotations

				return super.visitAnnotation(descriptor, visible);
			} else if (descriptor.startsWith("Ljava")) {
				// Java annotations are ignored

				return super.visitAnnotation(descriptor, visible);
			} else if (isKotlinMetadata(descriptor)) {
				// Ignore Kotlin metadata

				return super.visitAnnotation(descriptor, visible);
			}

			Patchwork.LOGGER.warn("Unknown field annotation: " + descriptor + " " + visible);
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	static class MethodScanner extends MethodVisitor {
		MethodScanner(MethodVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")) {
				return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
			} else if (descriptor.startsWith("Ljava")) {
				// Java annotations are ignored

				return super.visitAnnotation(descriptor, visible);
			} else if (descriptor.equals("Lorg/jetbrains/annotations/NotNull;") || descriptor.equals("Lorg/jetbrains/annotations/Nullable;")) {
				// Ignore @NotNull / @Nullable annotations

				return super.visitAnnotation(descriptor, visible);
			} else if (isKotlinMetadata(descriptor)) {
				// Ignore Kotlin metadata

				return super.visitAnnotation(descriptor, visible);
			} else {
				Patchwork.LOGGER.warn("Unknown method annotation: " + descriptor + " " + visible);
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

			Patchwork.LOGGER.warn("    %s -> %s", name, value);
		}
	}
}
