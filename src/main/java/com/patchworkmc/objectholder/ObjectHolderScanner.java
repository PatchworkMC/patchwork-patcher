package com.patchworkmc.objectholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.patchworkmc.Patchwork;
import com.patchworkmc.annotation.StringAnnotationHandler;
import com.patchworkmc.objectholder.initialization.VanillaRegistry;
import com.patchworkmc.util.LambdaVisitors;

public class ObjectHolderScanner extends ClassVisitor {
	// net.minecraft.util.Registry
	private static final String REGISTRY = "net/minecraft/class_2378";
	private static final String REGISTER_DESCRIPTOR = "(L" + REGISTRY + ";Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V";
	private static final String REGISTER_DYNAMIC_DESCRIPTOR = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Consumer;)V";

	private static final String OBJECT_HOLDER = "Lnet/minecraftforge/registries/ObjectHolder;";
	private static final String MOD = "Lnet/minecraftforge/fml/common/Mod;";

	private static final String PREFIX = "patchwork$objectHolder$";

	private static final int EXPECTED_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
	private List<ObjectHolder> holders;
	private boolean scanAllFields;
	private String defaultModId;
	private String className;

	public ObjectHolderScanner(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);

		holders = new ArrayList<>();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		this.className = name;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(OBJECT_HOLDER)) {
			// An explicit class-level @ObjectHolder annotation sets a default mod id for all field-level @ObjectHolder
			// annotations. Thus, they don't need to specify it explicitly.

			return new StringAnnotationHandler(value -> {
				this.defaultModId = value;
				this.scanAllFields = true;
			});
		} else if (descriptor.equals(MOD) && !this.scanAllFields) {
			// If there wasn't an explicit class-level @ObjectHolder annotation, but there is an @Mod annotation,
			// use it as the default mod id.

			return new StringAnnotationHandler(super.visitAnnotation(descriptor, visible), value -> {
				this.defaultModId = value;
			});
		} else {
			return super.visitAnnotation(descriptor, visible);
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor parent = super.visitField(access, name, descriptor, signature, value);

		return new FieldScanner(parent, access, name, descriptor);
	}

	@Override
	public void visitEnd() {
		generateSetters();

		if (!holders.isEmpty()) {
			MethodVisitor register = super.visitMethod(Opcodes.ACC_PUBLIC, "patchwork$registerObjectHolders", "()V", null, null);

			if (register != null) {
				generateObjectHolderRegistrations(register);
			}
		}

		super.visitEnd();
	}

	private void generateObjectHolderRegistrations(MethodVisitor method) {
		method.visitCode();

		method.visitFieldInsn(Opcodes.GETSTATIC, "net/patchworkmc/api/registries/ObjectHolderRegistry", "INSTANCE", "Lnet/patchworkmc/api/registries/ObjectHolderRegistry;");
		method.visitVarInsn(Opcodes.ASTORE, 1);

		Label start = new Label();
		method.visitLabel(start);

		for (ObjectHolder holder : holders) {
			// TODO: Code duplicated from generateSetters
			// Take the field value as the argument, return nothing
			String descriptor = "(" + holder.getDescriptor() + ")V";
			String name = PREFIX + holder.getField();

			VanillaRegistry registry = VanillaRegistry.get(holder.getDescriptor());

			String registerDescriptor = REGISTER_DESCRIPTOR;

			method.visitVarInsn(Opcodes.ALOAD, 1);

			if (registry == null) {
				if (holder.getDescriptor().startsWith("Lnet/minecraft/class_")) {
					Patchwork.LOGGER.warn("Don't know what registry the minecraft class " + holder.getDescriptor() + " belongs to, falling back to dynamic!");
				}

				method.visitLdcInsn(Type.getObjectType(holder.getDescriptor().substring(1, holder.getDescriptor().length() - 1)));
				registerDescriptor = REGISTER_DYNAMIC_DESCRIPTOR;
			} else {
				method.visitFieldInsn(Opcodes.GETSTATIC, REGISTRY, registry.getField(), registry.getFieldDescriptor());
			}

			method.visitLdcInsn(holder.getNamespace());
			method.visitLdcInsn(holder.getName());

			method.visitVarInsn(Opcodes.ALOAD, 0);
			LambdaVisitors.visitConsumerInstanceLambda(method, Opcodes.H_INVOKEVIRTUAL, className, name, descriptor, false);

			method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/patchworkmc/api/registries/ObjectHolderRegistry", "register", registerDescriptor, false);
		}

		Label end = new Label();
		method.visitLabel(end);

		method.visitInsn(Opcodes.RETURN);

		method.visitLocalVariable("registry", "Lnet/patchworkmc/api/registries/ObjectHolderRegistry;", null, start, end, 1);

		method.visitMaxs(6, 2);
		method.visitEnd();
	}

	private void generateSetters() {
		for (ObjectHolder holder : holders) {
			// Take the field value as the argument, return nothing
			String descriptor = "(" + holder.getDescriptor() + ")V";
			String name = PREFIX + holder.getField();

			MethodVisitor setter = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, descriptor, null, null);

			if (setter == null) {
				// Parent visitor chose to ignore the method
				continue;
			}

			setter.visitCode();

			// Load "this" on to the stack (1)
			setter.visitVarInsn(Opcodes.ALOAD, 0);
			// Load the field value on to the stack (2)
			setter.visitVarInsn(Opcodes.ALOAD, 1);
			// Set the field (0)
			setter.visitFieldInsn(Opcodes.PUTFIELD, className, holder.getField(), holder.getDescriptor());
			// Return
			setter.visitInsn(Opcodes.RETURN);

			setter.visitMaxs(2, 2);
			setter.visitEnd();
		}
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

				if ((access & Opcodes.ACC_STATIC) == 0) {
					Patchwork.LOGGER.error("Field " + name + " marked with an @ObjectHolder annotation was not static! All @ObjectHolder fields must be static.");

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

					holders.add(new ObjectHolder(name, this.descriptor, namespace, path));
				});
			} else {
				return super.visitAnnotation(descriptor, visible);
			}
		}

		@Override
		public void visitEnd() {
			super.visitEnd();

			if (!visited && defaultModId != null && scanAllFields && access == EXPECTED_ACCESS) {
				holders.add(new ObjectHolder(name, descriptor, defaultModId, name.toLowerCase(Locale.ENGLISH)));
			}
		}
	}
}
