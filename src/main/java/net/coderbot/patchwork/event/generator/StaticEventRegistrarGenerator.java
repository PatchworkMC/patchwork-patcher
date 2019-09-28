package net.coderbot.patchwork.event.generator;

import net.coderbot.patchwork.event.SubscribeEvent;
import net.coderbot.patchwork.generator.ConsumerGenerator;

import java.util.Collection;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class StaticEventRegistrarGenerator {
	// Class descriptor for IEventBus
	private static final String EVENT_BUS = "Lnet/minecraftforge/eventbus/api/IEventBus;";

	// Method descriptor for addListener()
	private static final String ADD_DESCRIPTOR =
			"(Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V";

	// Method descriptor for addGenericListener()
	private static final String ADD_GENERIC_DESCRIPTOR =
			"(Ljava/lang/Class;Lnet/minecraftforge/eventbus/api/EventPriority;ZLjava/lang/Class;Ljava/util/function/Consumer;)V";

	public static String generate(String targetClass,
			Collection<Map.Entry<String, SubscribeEvent>> entries,
			ClassVisitor visitor) {
		String generatedName = "patchwork_generated" + targetClass + "_StaticEventRegistrar";

		ConsumerGenerator generator =
				new ConsumerGenerator(visitor, generatedName, EVENT_BUS, null);

		// Note: Java would like us to emit INNERCLASS nodes here, however, there's no easy way to
		// do so without being error prone. Not emitting them seems to work fine in any case.

		// Add a default constructor
		generator.visitDefaultConstructor();

		// Add the accept implementation
		MethodVisitor method = generator.visitAccept();

		for(Map.Entry<String, SubscribeEvent> entry : entries) {
			String shimName = entry.getKey();
			SubscribeEvent subscriber = entry.getValue();
			boolean generic = subscriber.getSignature() != null;

			// Remove `(L` and `;)V`
			String descriptor = subscriber.getDescriptor();
			descriptor = descriptor.substring(2, descriptor.length() - 3);

			// Load the IEventBus object on to the stack
			method.visitVarInsn(Opcodes.ALOAD, 1);

			// Compute the generic class name if this is a generic event handler
			if(generic) {
				String signature = subscriber.getSignature();
				int start = signature.indexOf('<');
				int end = signature.lastIndexOf('>');

				// Remove the parts around the <> and the L and ; in one go
				signature = signature.substring(start + 2, end - 1);

				int trailingGeneric = signature.indexOf('<');

				if(trailingGeneric != -1) {
					signature = signature.substring(0, trailingGeneric);
				}

				method.visitLdcInsn(Type.getObjectType(signature));
			}

			// Adds the event priority
			method.visitFieldInsn(Opcodes.GETSTATIC,
					"net/minecraftforge/eventbus/api/EventPriority",
					subscriber.getPriority(),
					"Lnet/minecraftforge/eventbus/api/EventPriority;");

			// Loads 1 (true) if the subscriber wants to receive cancelled events, 0 (false)
			// otherwise
			method.visitInsn(subscriber.receiveCancelled() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);

			method.visitLdcInsn(Type.getObjectType(descriptor));

			method.visitTypeInsn(Opcodes.NEW, shimName);
			method.visitInsn(Opcodes.DUP);

			method.visitMethodInsn(Opcodes.INVOKESPECIAL, shimName, "<init>", "()V", false);

			method.visitTypeInsn(Opcodes.CHECKCAST, "java/util/function/Consumer");

			method.visitMethodInsn(Opcodes.INVOKEINTERFACE,
					"net/minecraftforge/eventbus/api/IEventBus",
					generic ? "addGenericListener" : "addListener",
					generic ? ADD_GENERIC_DESCRIPTOR : ADD_DESCRIPTOR,
					true);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(7, 2);
		method.visitEnd();

		// Add the bridge method and finish the visitor
		generator.visitEnd();

		return generatedName;
	}
}
