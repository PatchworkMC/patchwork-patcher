package net.coderbot.patchwork.at;

import org.objectweb.asm.*;

public class AccessorInterfaceGenerator {

	public static void generate(ModGutter.Meta meta, ClassWriter writer) {
		// Magic to get the parent class

		writer.visit(Opcodes.V1_8,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				"patchwork_generated/mixin/" + meta.getName() + "Accessor",
				null,
				"java/lang/Object",
				null);
		//@Mixin(targetclass.class)
		AnnotationVisitor mixinAnnotationVisitor =
				writer.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", false);
		{
			AnnotationVisitor mixinAnnotationValueVisitor =
					mixinAnnotationVisitor.visitArray("value");
			mixinAnnotationValueVisitor.visit(null,
					Type.getType("L" + meta.getAccessTransformerEntry().getClazzName() + ";"));
			mixinAnnotationValueVisitor.visitEnd();
		}
		mixinAnnotationVisitor.visitEnd();
		if(!meta.getDescriptor().contains("(") /* it's probably not a field*/) {
			if((meta.getOpcode() & Opcodes.ACC_STATIC) != 0) /*if is static*/ {
				MethodVisitor getter = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
						"get" + meta.getName(),
						"()" + meta.getName() + ";",
						null,
						null);
				// Mark getter with @Accessor
				getter.visitAnnotation("Lorg/spongepowered/asm/mixin/gen/Accessor;", true)
						.visitEnd();
				// Generate boilerplate that will be replaced by mixin stuff later todo might be
				// unnecessary
				getter.visitCode();
				getter.visitLabel(new Label());
				getter.visitInsn(Opcodes.ACONST_NULL);
				getter.visitInsn(Opcodes.ARETURN);
				getter.visitMaxs(1, 0);
				getter.visitEnd();

				// Generate the setter
				MethodVisitor setter = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
						"set" + meta.getName(),
						"(" + meta.getDescriptor() + ")V",
						null,
						null);
				// Mark setter with @Accessor
				setter.visitAnnotation("Lorg/spongepowered/asm/mixin/gen/Accessor;", true)
						.visitEnd();
				// Generate boilerplate to be replaced by mixin later
				setter.visitCode();
				Label methodStart = new Label();
				setter.visitLabel(methodStart);
				setter.visitInsn(Opcodes.RETURN);
				Label otherLabel /*i dont know what to call it*/ = new Label();
				setter.visitLabel(otherLabel);
				setter.visitLocalVariable(
						"var1", meta.getDescriptor(), null, methodStart, otherLabel, 0);
				setter.visitMaxs(0, 1);
				setter.visitEnd();
			} else {
				MethodVisitor getter = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"get" + meta.getName(),
						"()" + meta.getDescriptor(),
						null,
						null);
				// Mark getter with @Accessor
				getter.visitAnnotation("Lorg/spongepowered/asm/mixin/gen/Accessor;", true)
						.visitEnd();
				getter.visitEnd();

				// Generate the setter
				MethodVisitor setter = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
						"set" + meta.getName(),
						"(" + meta.getDescriptor() + ")V",
						null,
						null);
				// Mark setter with @Accessor
				setter.visitAnnotation("Lorg/spongepowered/asm/mixin/gen/Accessor;", true)
						.visitEnd();
				setter.visitEnd();
			}
		}
	}
}
