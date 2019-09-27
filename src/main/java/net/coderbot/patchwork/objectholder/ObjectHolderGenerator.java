package net.coderbot.patchwork.objectholder;

import net.coderbot.patchwork.generator.ConsumerGenerator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ObjectHolderGenerator {
	public static GeneratedEntry generate(String targetClass, ObjectHolders.Entry entry, ClassVisitor visitor) {
		GeneratedEntry generated = new GeneratedEntry(entry, targetClass);
		ConsumerGenerator generator = new ConsumerGenerator(visitor, generated.getShimName(), generated.getDescriptor());

		// Add a default constructor
		generator.visitDefaultConstructor();

		// Add the accept implementation
		MethodVisitor method = generator.visitAccept();

		{
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

		// Add the bridge method and finish the visitor
		generator.visitEnd();

		return generated;
	}

	public static class GeneratedEntry extends ObjectHolders.Entry {
		private String shimName;

		private GeneratedEntry(ObjectHolders.Entry entry, String baseName) {
			super(entry);

			this.shimName = "patchwork_generated" + baseName + "_ObjectHolder_" + entry.getField();
		}

		public String getShimName() {
			return shimName;
		}
	}
}
