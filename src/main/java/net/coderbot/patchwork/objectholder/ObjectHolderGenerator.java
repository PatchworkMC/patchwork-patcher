package net.coderbot.patchwork.objectholder;

import jdk.internal.org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ObjectHolderGenerator {
	public static GeneratedEntry generate(String targetClass, ObjectHolders.Entry entry, ClassVisitor visitor) {
		GeneratedEntry generated = new GeneratedEntry(entry, targetClass);

		visitor.visit(
				Opcodes.V1_8,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
				generated.getShimName(),
				"Ljava/lang/Object;Ljava/util/function/Consumer<" + entry.getDescriptor() + ">;",
				"java/lang/Object",
				new String[] { "java/util/function/Consumer" }
		);

		{
			MethodVisitor method = visitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			method.visitVarInsn(Opcodes.ALOAD, 0);
			method.visitMethodInsn(Opcodes.INVOKESPECIAL,
					"java/lang/Object",
					"<init>",
					"()V",
					false
			);
			method.visitInsn(Opcodes.RETURN);
			method.visitMaxs(1, 1);
			method.visitEnd();
		}

		{
			MethodVisitor method = visitor.visitMethod(
					Opcodes.ACC_PUBLIC,
					"accept",
					"(" + entry.getDescriptor() + ")V",
					null,
					null
			);

			method.visitVarInsn(
					Opcodes.ALOAD,
					1
			);

			method.visitFieldInsn(
					Opcodes.PUTSTATIC,
					targetClass.substring(1),
					entry.getField(),
					entry.getDescriptor()
			);

			method.visitInsn(Opcodes.RETURN);

			method.visitMaxs(1, 2);
			method.visitEnd();
		}

		// Bridge method

		{
			MethodVisitor method = visitor.visitMethod(
					Opcodes.ACC_PUBLIC,
					"accept",
					"(Ljava/lang/Object;)V",
					null,
					null
			);

			method.visitVarInsn(
					Opcodes.ALOAD,
					0
			);

			method.visitVarInsn(
					Opcodes.ALOAD,
					1
			);

			method.visitTypeInsn(
					Opcodes.CHECKCAST,
					entry.getDescriptor().substring(1, entry.getDescriptor().length() - 1)
			);

			method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
					generated.getShimName(),
					"accept",
					"(" + entry.getDescriptor() + ")V",
					false
			);

			method.visitInsn(Opcodes.RETURN);

			method.visitMaxs(2, 2);
			method.visitEnd();
		}

		visitor.visitEnd();

		return generated;
	}

	public static class GeneratedEntry extends ObjectHolders.Entry {
		private String shimName;

		private GeneratedEntry(ObjectHolders.Entry entry, String baseName) {
			super(entry);

			this.shimName = "patchwork_generated" + baseName + "ObjectHolder_" + entry.getField();
		}

		public String getShimName() {
			return shimName;
		}
	}
}
