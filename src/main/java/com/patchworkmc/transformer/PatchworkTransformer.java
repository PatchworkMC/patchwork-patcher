package com.patchworkmc.transformer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.patchworkmc.Patchwork;
import com.patchworkmc.access.AccessTransformation;
import com.patchworkmc.access.AccessTransformations;
import com.patchworkmc.access.AccessTransformer;
import com.patchworkmc.annotation.AnnotationProcessor;
import com.patchworkmc.event.EventBusSubscriber;
import com.patchworkmc.event.EventHandlerScanner;
import com.patchworkmc.event.SubscribeEvent;
import com.patchworkmc.event.generator.InstanceEventRegistrarGenerator;
import com.patchworkmc.event.generator.StaticEventRegistrarGenerator;
import com.patchworkmc.event.generator.SubscribeEventGenerator;
import com.patchworkmc.logging.Logger;
import com.patchworkmc.objectholder.ForgeInitializerGenerator;
import com.patchworkmc.objectholder.ObjectHolder;
import com.patchworkmc.objectholder.ObjectHolderGenerator;
import com.patchworkmc.objectholder.ObjectHolderScanner;
import com.patchworkmc.patch.BlockSettingsTransformer;
import com.patchworkmc.patch.ItemGroupTransformer;

public class PatchworkTransformer implements BiConsumer<String, byte[]> {
	private static final Logger LOGGER = Patchwork.LOGGER;

	private BiConsumer<String, byte[]> outputConsumer;
	private boolean finished;

	private Queue<Map.Entry<String, ObjectHolder>> generatedObjectHolderEntries = new ConcurrentLinkedQueue<>(); // shimName -> ObjectHolder
	private Queue<Map.Entry<String, String>> staticEventRegistrars = new ConcurrentLinkedQueue<>(); // shimName -> baseName
	private Queue<Map.Entry<String, String>> instanceEventRegistrars = new ConcurrentLinkedQueue<>(); // shimName -> baseName
	private Queue<Map.Entry<String, EventBusSubscriber>> eventBusSubscribers = new ConcurrentLinkedQueue<>(); // basename -> EventBusSubscriber
	private Queue<Map.Entry<String, String>> modInfo = new ConcurrentLinkedQueue<>(); // modId -> clazz

	public PatchworkTransformer(BiConsumer<String, byte[]> outputConsumer) {
		this.outputConsumer = outputConsumer;
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

		if (name.startsWith("net/minecraft")) {
			throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Minecraft's package!");
		}

		if (name.startsWith("java")) {
			throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Java's package!");
		}

		ClassReader reader = new ClassReader(content);
		ClassNode node = new ClassNode();

		List<ObjectHolder> objectHolders = new ArrayList<>();
		List<SubscribeEvent> subscribeEvents = new ArrayList<>();

		AccessTransformations accessTransformations = new AccessTransformations();

		Consumer<String> modConsumer = classModId -> {
			LOGGER.trace("Found @Mod annotation at " + name + " (id: " + classModId + ")");
			modInfo.add(new AbstractMap.SimpleImmutableEntry<>(classModId, name));
		};

		AnnotationProcessor scanner = new AnnotationProcessor(node, modConsumer);
		ObjectHolderScanner objectHolderScanner = new ObjectHolderScanner(scanner, holder -> {
			objectHolders.add(holder);

			accessTransformations.addFieldTransformation(holder.getField(), AccessTransformation.DEFINALIZE_MAKE_PUBLIC);
		});

		EventHandlerScanner eventHandlerScanner = new EventHandlerScanner(objectHolderScanner, subscriber ->
				eventBusSubscribers.add(new AbstractMap.SimpleImmutableEntry<>(name, subscriber)), subscribeEvent -> {
			subscribeEvents.add(subscribeEvent);

			accessTransformations.setClassTransformation(AccessTransformation.MAKE_PUBLIC);

			accessTransformations.addMethodTransformation(subscribeEvent.getMethod(), subscribeEvent.getMethodDescriptor(), AccessTransformation.MAKE_PUBLIC);
		});

		ItemGroupTransformer itemGroupTransformer = new ItemGroupTransformer(eventHandlerScanner);
		BlockSettingsTransformer blockSettingsTransformer = new BlockSettingsTransformer(itemGroupTransformer);

		reader.accept(blockSettingsTransformer, ClassReader.EXPAND_FRAMES);

		ClassWriter writer = new ClassWriter(0);
		AccessTransformer accessTransformer = new AccessTransformer(writer, accessTransformations);

		node.accept(accessTransformer);

		objectHolders.forEach(entry -> {
			ClassWriter shimWriter = new ClassWriter(0);
			String shimName = ObjectHolderGenerator.generate(name, entry, shimWriter);

			generatedObjectHolderEntries.add(new AbstractMap.SimpleImmutableEntry<>(shimName, entry));

			outputConsumer.accept(shimName, shimWriter.toByteArray());
		});

		HashMap<String, SubscribeEvent> subscribeEventStaticShims = new HashMap<>();
		HashMap<String, SubscribeEvent> subscribeEventInstanceShims = new HashMap<>();

		subscribeEvents.forEach(entry -> {
			ClassWriter shimWriter = new ClassWriter(0);
			String shimName = SubscribeEventGenerator.generate(name, entry, shimWriter);

			if (subscribeEventStaticShims.containsKey(shimName) || subscribeEventInstanceShims.containsKey(shimName)) {
				throw new UnsupportedOperationException("FIXME: Two @SubscribeEvent shims have the same name! This should be handled by Patchwork, it's a bug!");
			}

			if ((entry.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
				subscribeEventStaticShims.put(shimName, entry);
			} else {
				subscribeEventInstanceShims.put(shimName, entry);
			}

			outputConsumer.accept(shimName, shimWriter.toByteArray());
		});

		if (!subscribeEventStaticShims.isEmpty()) {
			ClassWriter shimWriter = new ClassWriter(0);
			String shimName = StaticEventRegistrarGenerator.generate(name, subscribeEventStaticShims.entrySet(), shimWriter);

			outputConsumer.accept(shimName, shimWriter.toByteArray());

			staticEventRegistrars.add(new AbstractMap.SimpleImmutableEntry<>(shimName, name));
		}

		if (!subscribeEventInstanceShims.isEmpty()) {
			ClassWriter shimWriter = new ClassWriter(0);
			String shimName = InstanceEventRegistrarGenerator.generate(name, subscribeEventInstanceShims.entrySet(), shimWriter);

			outputConsumer.accept(shimName, shimWriter.toByteArray());

			instanceEventRegistrars.add(new AbstractMap.SimpleImmutableEntry<>(shimName, name));
		}

		outputConsumer.accept(name, writer.toByteArray());
	}

	/**
	 * Finishes the patching process.
	 *
	 * @param entrypoints outputs the list of entrypoints for the fabric.mod.json
	 * @return the primary mod id
	 */
	public String finish(Consumer<String> entrypoints) {
		if (finished) {
			throw new IllegalStateException("Already finished!");
		}

		this.finished = true;

		if (modInfo.isEmpty()) {
			throw new IllegalStateException("Located no classes with an @Mod annotation, could not pick a primary mod!");
		}

		Map.Entry<String, String> primary = modInfo.peek();
		String primaryId = primary.getKey();
		String primaryClazz = primary.getValue();

		generateInitializer(primaryId, primaryClazz, entrypoints);

		staticEventRegistrars.clear();
		instanceEventRegistrars.clear();
		eventBusSubscribers.clear();
		generatedObjectHolderEntries.clear();

		modInfo.forEach(entry -> {
			if (entry.getKey().equals(primaryId)) {
				return;
			}

			generateInitializer(entry.getKey(), entry.getValue(), entrypoints);
		});

		return primaryId;
	}

	private void generateInitializer(String id, String clazz, Consumer<String> entrypoints) {
		ClassWriter initializerWriter = new ClassWriter(0);
		String initializerName = "patchwork_generated/" + clazz + "Initializer";
		ForgeInitializerGenerator.generate(clazz, initializerName, id, staticEventRegistrars, instanceEventRegistrars, eventBusSubscribers, generatedObjectHolderEntries, initializerWriter);

		entrypoints.accept(initializerName.replace('/', '.'));
		outputConsumer.accept(initializerName, initializerWriter.toByteArray());
	}
}
