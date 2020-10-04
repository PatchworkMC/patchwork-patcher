package net.patchworkmc.patcher.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.tinyremapper.OutputConsumerPath;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.annotation.AnnotationProcessor;
import net.patchworkmc.patcher.capabilityinject.CapabilityInjectRewriter;
import net.patchworkmc.patcher.event.EventBusSubscriber;
import net.patchworkmc.patcher.event.EventHandlerRewriter;
import net.patchworkmc.patcher.event.EventSubscriptionChecker;
import net.patchworkmc.patcher.event.SubscribingClass;
import net.patchworkmc.patcher.mapping.remapper.PatchworkRemapper;
import net.patchworkmc.patcher.objectholder.ObjectHolder;
import net.patchworkmc.patcher.objectholder.ObjectHolderRewriter;
import net.patchworkmc.patcher.patch.StringConstantRemapper;
import net.patchworkmc.patcher.transformer.api.Transformer;
import net.patchworkmc.patcher.transformer.api.Transformers;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class PatchworkTransformer implements BiConsumer<String, byte[]> {
	private static final Logger LOGGER = Patchwork.LOGGER;

	private final OutputConsumerPath outputConsumer;
	private final PatchworkRemapper remapper;

	private final Set<String> objectHolderClasses = ConcurrentHashMap.newKeySet();
	private final Set<String> capabilityInjectClasses = ConcurrentHashMap.newKeySet();
	private final Set<SubscribingClass> subscribingClasses = ConcurrentHashMap.newKeySet();
	// Queues are used instead of another collection type because they have concurrency
	private final Queue<Map.Entry<String, EventBusSubscriber>> eventBusSubscribers = new ConcurrentLinkedQueue<>(); // basename -> EventBusSubscriber
	private final Queue<Map.Entry<String, String>> modInfo = new ConcurrentLinkedQueue<>(); // modId -> clazz

	private final EventSubscriptionChecker checker = new EventSubscriptionChecker();
	private final ForgeModJar modJar;

	private boolean finished;

	/**
	 * The main class transformer for Patchwork.
	**/
	public PatchworkTransformer(OutputConsumerPath outputConsumer, PatchworkRemapper remapper, ForgeModJar modJar) {
		this.outputConsumer = outputConsumer;
		this.remapper = remapper;
		this.modJar = modJar;
		this.finished = false;
	}

	@Override
	public void accept(String name, byte[] content) {
		// Names should match Java internal names, such as "java/lang/Object" or "com/example/Example$1"

		if (name.startsWith("/")) {
			throw new IllegalArgumentException("Name should not start with a /");
		}

		if (name.endsWith(".class")) {
			throw new IllegalArgumentException("Name should not end with .class");
		}

		if (name.startsWith("net/minecraft/")) {
			throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Minecraft's package!");
		}

		if (name.startsWith("java/")) {
			throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Java's package!");
		}
		// TODO: BEFORE MERGE: get the correct MinecraftVersion
		outputConsumer.accept(name, Transformers.apply(MinecraftVersion.V1_14_4, modJar, content));
	}

// TODO: inhertience checker

	public OutputConsumerPath getOutputConsumer() {
		return outputConsumer;
	}

	public void closeOutputConsumer() {
		try {
			this.outputConsumer.close();
		} catch (IOException ex) {
			throw new UncheckedIOException("Unable to close OutputConsumerPath", ex);
		}
	}
}
