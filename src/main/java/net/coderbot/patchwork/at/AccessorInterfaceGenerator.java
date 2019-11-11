package net.coderbot.patchwork.at;

import org.objectweb.asm.*;

public class AccessorInterfaceGenerator {

	public static void generate(String modid, ModGutter.Meta meta, ClassWriter writer) {
		// Magic to get the parent class

		writer.visit(Opcodes.V1_8,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
				"patchwork_generated/" + modid + "/mixin/" + meta.getName() + "Accessor",
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
				// Throw an IllegalStateException if the mixin wasn't applied
				getter.visitCode();
				getter.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException");
				getter.visitInsn(Opcodes.DUP);
				getter.visitMethodInsn(Opcodes.INVOKESPECIAL,
						"java/lang/IllegalStateException",
						"<init>",
						"()V",
						false);
				getter.visitInsn(Opcodes.ATHROW);
				getter.visitMaxs(2, 0);
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
				// Generate boilerplate
				setter.visitCode();
				// Throw an IllegalStateException if the mixin wasn't applied
				setter.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException");
				setter.visitInsn(Opcodes.DUP);
				setter.visitMethodInsn(Opcodes.INVOKESPECIAL,
						"java/lang/IllegalStateException",
						"<init>",
						"()V",
						false);
				setter.visitInsn(Opcodes.ATHROW);
				// add the local variable
				setter.visitLocalVariable("var1", meta.getDescriptor(), null, null, null, 0);
				setter.visitMaxs(2, 1);
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
