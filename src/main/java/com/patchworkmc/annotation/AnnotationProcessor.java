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
	private AnnotationStorage annotationStorage;
	private String className;

	public AnnotationProcessor(ClassVisitor parent, Consumer<String> consumer, AnnotationStorage annotationStorage) {
		super(Opcodes.ASM7, parent);

		this.consumer = consumer;
		this.annotationStorage = annotationStorage;
	}

	private static boolean isKotlinMetadata(String descriptor) {
		// TODO: This is specific to one mod
		return descriptor.startsWith("Lcom/greenapple/glacia/embedded/kotlin/");
	}

	private static boolean isForgeAnnotation(String descriptor) {
		return descriptor.startsWith("Lnet/minecraftforge/");
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		className = name;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		annotationStorage.acceptClassAnnotation(descriptor, className);

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
		} else if (isForgeAnnotation(descriptor)) {
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}

		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor parent = super.visitField(access, name, descriptor, signature, value);
		return new FieldScanner(parent, annotationStorage, className, name);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodScanner(parent, annotationStorage, className, name + descriptor);
	}

	static class FieldScanner extends FieldVisitor {
		private AnnotationStorage annotationStorage;
		private String outerClass;
		private String fieldName;

		FieldScanner(FieldVisitor parent, AnnotationStorage annotationStorage, String outerClass, String fieldName) {
			super(Opcodes.ASM7, parent);
			this.annotationStorage = annotationStorage;
			this.outerClass = outerClass;
			this.fieldName = fieldName;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			annotationStorage.acceptFieldAnnotation(descriptor, outerClass, fieldName);

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
			} else if (isForgeAnnotation(descriptor)) {
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
			}

			return super.visitAnnotation(descriptor, visible);
		}
	}

	static class MethodScanner extends MethodVisitor {
		private AnnotationStorage annotationStorage;
		private String outerClass;
		private String method;

		MethodScanner(MethodVisitor parent, AnnotationStorage annotationStorage, String outerClass, String method) {
			super(Opcodes.ASM7, parent);
			this.annotationStorage = annotationStorage;
			this.outerClass = outerClass;
			this.method = method;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			annotationStorage.acceptMethodAnnotation(descriptor, outerClass, method);

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
			} else if (isForgeAnnotation(descriptor)) {
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
			}

			return super.visitAnnotation(descriptor, visible);
		}
	}

	static class AnnotationPrinter extends AnnotationVisitor {
		AnnotationPrinter(AnnotationVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);

			Patchwork.LOGGER.warn("Unknown Forge Annotation %s -> %s", name, value);
		}
	}
}
