package com.patchworkmc.objectholder;

import java.util.Locale;
import java.util.function.Consumer;

import com.patchworkmc.annotation.StringAnnotationHandler;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class ObjectHolderScanner extends ClassVisitor {
	private static final String OBJECT_HOLDER = "Lnet/minecraftforge/registries/ObjectHolder;";
	private static int EXPECTED_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
	private Consumer<ObjectHolder> consumer;
	private String defaultModId;

	public ObjectHolderScanner(ClassVisitor parent, Consumer<ObjectHolder> consumer) {
		super(Opcodes.ASM7, parent);

		this.consumer = consumer;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(OBJECT_HOLDER)) {
			return new StringAnnotationHandler(value -> this.defaultModId = value);
		} else {
			return super.visitAnnotation(descriptor, visible);
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor parent = super.visitField(access, name, descriptor, signature, value);

		return new FieldScanner(parent, access, name, descriptor);
	}

	class FieldScanner extends FieldVisitor {
		private int access;
		private String name;
		private String descriptor;
		private boolean visited;

		FieldScanner(FieldVisitor parent, int access, String name, String descriptor) {
			super(Opcodes.ASM7, parent);

			this.access = access;
			this.name = name;
			this.descriptor = descriptor;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.equals(OBJECT_HOLDER)) {
				// Compare without taking into consideration the final flag here.
				// Apparently it's valid to not have `final` here, so we'll ignore it.

				if ((access & Opcodes.ACC_FINAL) != EXPECTED_ACCESS) {
					System.err.println("Field " + name + " marked with an @ObjectHolder annotation did not have the expected access of public static final, skipping: " + access);

					return null;
				}

				visited = true;

				return new StringAnnotationHandler(value -> {
					String namespace;
					String path;

					if (value.contains(":")) {
						String[] parts = value.split(":");

						namespace = parts[0];
						path = parts[1];
					} else if (defaultModId != null) {
						namespace = defaultModId;
						path = value;
					} else {
						throw new IllegalArgumentException("Field " + name + " marked with an @ObjectHolder annotation did not contain a mod id in a class level @ObjectHolder annotation");
					}

					consumer.accept(new ObjectHolder(name, this.descriptor, namespace, path));
				});
			} else {
				return super.visitAnnotation(descriptor, visible);
			}
		}

		@Override
		public void visitEnd() {
			super.visitEnd();

			if (!visited && defaultModId != null && access == EXPECTED_ACCESS) {
				consumer.accept(new ObjectHolder(name, descriptor, defaultModId, name.toLowerCase(Locale.ENGLISH)));
			}
		}
	}
}
