package net.coderbot.patchwork.objectholder;

import net.coderbot.patchwork.annotation.AnnotationConsumer;
import net.coderbot.patchwork.annotation.StringAnnotationHandler;
import org.objectweb.asm.*;

public class ObjectHolderScanPass extends ClassVisitor {
	private static final String OBJECT_HOLDER = "Lnet/minecraftforge/registries/ObjectHolder;";

	private AnnotationConsumer consumer;

	public ObjectHolderScanPass(ClassVisitor parent, AnnotationConsumer consumer) {
		super(Opcodes.ASM7, parent);

		this.consumer = consumer;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if(descriptor.equals(OBJECT_HOLDER)) {
			return new StringAnnotationHandler(consumer::acceptObjectHolder);
		} else {
			return super.visitAnnotation(descriptor, visible);
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return new FieldScanner(super.visitField(access, name, descriptor, signature, value), name, descriptor, consumer);
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
			if(descriptor.equals(OBJECT_HOLDER)) {
				System.err.println("not fully handled annotation: "+descriptor+" "+visible);
				return new StringAnnotationHandler(value -> consumer.acceptObjectHolder(name, value));
			} else {
				return super.visitAnnotation(descriptor, visible);
			}
		}
	}
}
