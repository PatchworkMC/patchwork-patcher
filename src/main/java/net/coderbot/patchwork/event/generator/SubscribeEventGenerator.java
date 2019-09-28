package net.coderbot.patchwork.event.generator;

import net.coderbot.patchwork.event.SubscribeEvent;
import net.coderbot.patchwork.generator.ConsumerGenerator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SubscribeEventGenerator {
	public static String generate(String targetClass, SubscribeEvent entry, ClassVisitor visitor) {
		if((entry.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
			return generateStatic(targetClass, entry, visitor);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private static String generateStatic(String targetClass,
			SubscribeEvent entry,
			ClassVisitor visitor) {

		String descriptor = "L" + entry.getEventClass() + ";";
		String signature =
				entry.getGenericClass()
						.map(genericClass
								-> "L" + entry.getEventClass() + "<L" + genericClass + ";>;")
						.orElse(null);

		System.out.println(descriptor + " " + signature);

		String shimName =
				"patchwork_generated" + targetClass + "_SubscribeEvent_" + entry.getMethod();

		ConsumerGenerator generator =
				new ConsumerGenerator(visitor, shimName, descriptor, signature);

		// Add a default constructor
		generator.visitDefaultConstructor();

		// Add the accept implementation
		MethodVisitor method = generator.visitAccept();

		{
			method.visitVarInsn(Opcodes.ALOAD, 1);

			method.visitMethodInsn(Opcodes.INVOKESTATIC,
					targetClass.substring(1),
					entry.getMethod(),
					"(" + descriptor + ")V",
					false);

			method.visitInsn(Opcodes.RETURN);

			method.visitMaxs(1, 2);
			method.visitEnd();
		}

		// Add the bridge method and finish the visitor
		generator.visitEnd();

		return shimName;
	}
}
