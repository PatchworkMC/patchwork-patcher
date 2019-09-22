package net.coderbot.patchwork.annotation;

import org.objectweb.asm.*;
import org.spongepowered.asm.lib.Opcodes;

public class AnnotationProcessor extends ClassVisitor {
	private ForgeAnnotations annotations;

	public AnnotationProcessor(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);

		this.annotations = new ForgeAnnotations();
	}

	public ForgeAnnotations getAnnotations() {
		return annotations;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		System.out.println("class annotation: "+descriptor+" "+visible);

		switch (descriptor) {
			case "Lnet/minecraftforge/fml/common/Mod;":
				// Strip this annotation
				return new Mod.Handler(annotations);
			case "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;":
				// Strip this annotation
				return new EventBusSubscriber.Handler(annotations);
			default:
				System.err.println("Unknown class annotation!");
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return new FieldScanner(super.visitField(access, name, descriptor, signature, value));
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		System.out.println(access + " " + name + " " + descriptor + " " + signature);

		return new MethodScanner(super.visitMethod(access, name, descriptor, signature, exceptions), name, descriptor, signature);
	}

	public static class FieldScanner extends FieldVisitor {
		public FieldScanner(FieldVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			System.out.println("field annotation: "+descriptor+" "+visible);

			return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	public class MethodScanner extends MethodVisitor {
		String name;
		String descriptor;
		String signature;

		public MethodScanner(MethodVisitor parent, String name, String descriptor, String signature) {
			super(Opcodes.ASM7, parent);

			this.name = name;
			this.descriptor = descriptor;
			this.signature = signature;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			System.out.println("method annotation: "+descriptor+" "+visible);

			switch(descriptor) {
				case "Lnet/minecraftforge/eventbus/api/SubscribeEvent;":
					return new SubscribeEvent.Handler(this.name, this.descriptor, this.signature, annotations);
				case "Lnet/minecraftforge/api/distmarker/OnlyIn;":
					return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
				default:
					return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
			}
		}
	}

	public static class AnnotationPrinter extends AnnotationVisitor {
		public AnnotationPrinter(AnnotationVisitor parent) {
			super(Opcodes.ASM7, parent);
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);

			System.out.println(name + "->" + value);
		}
	}
}
