package net.coderbot.patchwork.event.generator;

import net.coderbot.patchwork.event.SubscribeEvent;
import net.coderbot.patchwork.generator.ConsumerGenerator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SubscribeEventGenerator {
	public static String generate(String targetClass, SubscribeEvent entry, ClassVisitor visitor) {
		if ((entry.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
			return generateStatic(targetClass, entry, visitor);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private static String generateStatic(String targetClass, SubscribeEvent entry, ClassVisitor visitor) {
		// Strip the ( prefix and the )V suffix
		// TODO: Verify single argument and )V ending
		String descriptor = entry.getDescriptor().substring(1, entry.getDescriptor().length() - 2);
		String signature = entry.getSignature() != null ? entry.getSignature().substring(1, entry.getSignature().length() - 2) : null;

		String shimName = "patchwork_generated" + targetClass + "_SubscribeEvent_" + entry.getMethod();

		ConsumerGenerator generator = new ConsumerGenerator(visitor, shimName, descriptor, signature);

		// Add a default constructor
		generator.visitDefaultConstructor();

		// Add the accept implementation
		MethodVisitor method = generator.visitAccept();

		{
			method.visitVarInsn(
					Opcodes.ALOAD,
					1
			);

			method.visitMethodInsn(
					Opcodes.INVOKESTATIC,
					targetClass.substring(1),
					entry.getMethod(),
					entry.getDescriptor(),
					false
			);

			method.visitInsn(Opcodes.RETURN);

			method.visitMaxs(1, 2);
			method.visitEnd();
		}

		// Add the bridge method and finish the visitor
		generator.visitEnd();

		return shimName;
	}
}
