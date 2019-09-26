package net.coderbot.patchwork.annotation;

import org.objectweb.asm.*;

public class AnnotationProcessor extends ClassVisitor {
	private ForgeAnnotations annotations;
	private AnnotationConsumer consumer;

	public AnnotationProcessor(ClassVisitor parent, AnnotationConsumer consumer) {
		super(Opcodes.ASM7, parent);

		this.annotations = new ForgeAnnotations();
		this.consumer = consumer;
	}

	public ForgeAnnotations getAnnotations() {
		return annotations;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		switch (descriptor) {
			case "Lnet/minecraftforge/fml/common/Mod;":
				return new StringAnnotationHandler(consumer::acceptMod);
			case "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;":
				return new EventBusSubscriberHandler(consumer);
			default:
				System.err.println("Unknown class annotation!");
				return new AnnotationPrinter(super.visitAnnotation(descriptor, visible));
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return new FieldScanner(super.visitField(access, name, descriptor, signature, value), name, descriptor, consumer);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodScanner(super.visitMethod(access, name, descriptor, signature, exceptions), name, descriptor, signature);
	}

	public static class FieldScanner extends FieldVisitor {
		String name;
		AnnotationConsumer consumer;

		public FieldScanner(FieldVisitor parent, String name, String descriptor, AnnotationConsumer consumer) {
			super(Opcodes.ASM7, parent);

			this.name = name;
			this.consumer = consumer;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			System.err.println("unknown field annotation: "+descriptor+" "+visible);
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
			switch(descriptor) {
				case "Lnet/minecraftforge/eventbus/api/SubscribeEvent;":
					return new SubscribeEvent.Handler(this.name, this.descriptor, this.signature, annotations);
				case "Lnet/minecraftforge/api/distmarker/OnlyIn;":
					return new OnlyInRewriter(super.visitAnnotation(OnlyInRewriter.TARGET_DESCRIPTOR, visible));
				default:
					System.err.println("unknown method annotation: "+descriptor+" "+visible);
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

			System.err.println("    " + name + "->" + value);
		}
	}
}
