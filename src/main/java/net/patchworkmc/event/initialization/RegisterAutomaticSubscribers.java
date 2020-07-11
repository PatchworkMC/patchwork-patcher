package net.patchworkmc.event.initialization;

import java.util.Map;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.patchworkmc.Patchwork;
import net.patchworkmc.event.EventBusSubscriber;

public class RegisterAutomaticSubscribers implements Consumer<MethodVisitor> {
	private Iterable<Map.Entry<String, EventBusSubscriber>> subscribers;

	public RegisterAutomaticSubscribers(Iterable<Map.Entry<String, EventBusSubscriber>> subscribers) {
		this.subscribers = subscribers;
	}

	@Override
	public void accept(MethodVisitor method) {
		for (Map.Entry<String, EventBusSubscriber> entry : subscribers) {
			String baseName = entry.getKey();
			EventBusSubscriber subscriber = entry.getValue();

			// TODO: Check targetModId

			if (!subscriber.isClient() || !subscriber.isServer()) {
				if (System.getProperty("patchwork:ignore_sided_annotations", "false").equals("true")) {
					Patchwork.LOGGER.warn("Sided @EventBusSubscriber annotations are not supported yet, applying " + subscriber + " from " + baseName + " without sides.");
				} else {
					Patchwork.LOGGER.error("Sided @EventBusSubscriber annotations are not supported yet, skipping: " + subscriber + " attached to: " + baseName);
					continue;
				}
			}

			if (subscriber.getBus() == EventBusSubscriber.Bus.MOD) {
				method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", "get", "()Lnet/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext;", false);

				method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext", "getModEventBus", "()Lnet/minecraftforge/eventbus/api/IEventBus;", false);
			} else {
				method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/common/MinecraftForge", "EVENT_BUS", "Lnet/minecraftforge/eventbus/api/IEventBus;");
			}

			method.visitLdcInsn(Type.getObjectType(baseName));

			method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/minecraftforge/eventbus/api/IEventBus", "register", "(Ljava/lang/Object;)V", true);
		}

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(2, 0);
		method.visitEnd();
	}
}
