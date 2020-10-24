package net.patchworkmc.patcher.annotation;


import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.transformer.api.ClassPostTransformer;
import net.patchworkmc.patcher.transformer.api.Transformer;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class AnnotationProcessor extends Transformer {
	private String className;

	public AnnotationProcessor(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer postTransformer) {
		super(version, jar, parent, postTransformer);
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
		this.forgeModJar.getAnnotationStorage().acceptClassAnnotation(descriptor, className);

		if (descriptor.equals("Lnet/minecraftforge/fml/common/Mod;")) {
			return new ForgeModAnnotationHandler(this.forgeModJar, this.className);
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
			Patchwork.LOGGER.warn("Unknown Forge class annotation: " + descriptor);
			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}

		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor parent = super.visitField(access, name, descriptor, signature, value);
		return new FieldScanner(parent, this.forgeModJar.getAnnotationStorage(), className, name);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, descriptor, signature, exceptions);

		if (name.equals("getForgeModId")) {
			throw new IllegalArgumentException("Someone used the reserved name 'getPatchworkModId'!");
		}

		return new MethodScanner(parent, this.forgeModJar.getAnnotationStorage(), className, name + descriptor);
	}

	static class FieldScanner extends FieldVisitor {
		private final AnnotationStorage annotationStorage;
		private final String outerClass;
		private final String fieldName;

		FieldScanner(FieldVisitor parent, AnnotationStorage annotationStorage, String outerClass, String fieldName) {
			super(Opcodes.ASM9, parent);
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
				Patchwork.LOGGER.warn("Unknown Forge field annotation: " + descriptor);
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
			}

			return super.visitAnnotation(descriptor, visible);
		}
	}

	static class MethodScanner extends MethodVisitor {
		private final AnnotationStorage annotationStorage;
		private final String outerClass;
		private final String method;

		MethodScanner(MethodVisitor parent, AnnotationStorage annotationStorage, String outerClass, String method) {
			super(Opcodes.ASM9, parent);
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
				Patchwork.LOGGER.warn("Unknown Forge method annotation: " + descriptor);
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
			}

			return super.visitAnnotation(descriptor, visible);
		}
	}

	static class AnnotationPrinter extends AnnotationVisitor {
		AnnotationPrinter(AnnotationVisitor parent) {
			super(Opcodes.ASM9, parent);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);

			Patchwork.LOGGER.warn("%s -> %s", name, value);
		}
	}
}
