package net.patchworkmc.patcher.transformer;

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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.access.ClassAccessWidenings;
import net.patchworkmc.patcher.annotation.AnnotationProcessor;
import net.patchworkmc.patcher.annotation.AnnotationStorage;
import net.patchworkmc.patcher.capabilityinject.CapabilityInject;
import net.patchworkmc.patcher.capabilityinject.CapabilityInjectRewriter;
import net.patchworkmc.patcher.capabilityinject.initialization.RegisterCapabilityInjects;
import net.patchworkmc.patcher.event.EventBusSubscriber;
import net.patchworkmc.patcher.event.EventHandlerRewriter;
import net.patchworkmc.patcher.event.EventSubclassTransformer;
import net.patchworkmc.patcher.event.EventSubscriptionChecker;
import net.patchworkmc.patcher.event.SubscribingClass;
import net.patchworkmc.patcher.event.initialization.RegisterAutomaticSubscribers;
import net.patchworkmc.patcher.event.initialization.RegisterEventRegistrars;
import net.patchworkmc.patcher.mapping.remapper.PatchworkRemapper;
import net.patchworkmc.patcher.objectholder.ObjectHolder;
import net.patchworkmc.patcher.objectholder.ObjectHolderRewriter;
import net.patchworkmc.patcher.objectholder.initialization.RegisterObjectHolders;
import net.patchworkmc.patcher.patch.BiomeLayersTransformer;
import net.patchworkmc.patcher.patch.BlockSettingsTransformer;
import net.patchworkmc.patcher.patch.ExtensibleEnumTransformer;
import net.patchworkmc.patcher.patch.ItemGroupTransformer;
import net.patchworkmc.patcher.patch.LevelGeneratorTypeTransformer;
import net.patchworkmc.patcher.patch.StringConstantRemapper;
import net.patchworkmc.patcher.transformer.initialization.ConstructTargetMod;

public class PatchworkTransformer implements BiConsumer<String, byte[]> {
	private static final Logger LOGGER = Patchwork.LOGGER;

	private final BiConsumer<String, byte[]> outputConsumer;
	private final PatchworkRemapper remapper;

	private final Set<String> objectHolderClasses = ConcurrentHashMap.newKeySet();
	private final Set<String> capabilityInjectClasses = ConcurrentHashMap.newKeySet();
	private final Set<SubscribingClass> subscribingClasses = ConcurrentHashMap.newKeySet();
	// Queues are used instead of another collection type because they have concurrency
	private final Queue<Map.Entry<String, EventBusSubscriber>> eventBusSubscribers = new ConcurrentLinkedQueue<>(); // basename -> EventBusSubscriber
	private final Queue<Map.Entry<String, String>> modInfo = new ConcurrentLinkedQueue<>(); // modId -> clazz

	private final EventSubscriptionChecker checker = new EventSubscriptionChecker();
	private final AnnotationStorage annotationStorage;

	private boolean finished;

	/**
	 * The main class transformer for Patchwork.
	 */
	public PatchworkTransformer(BiConsumer<String, byte[]> outputConsumer, PatchworkRemapper remapper, AnnotationStorage annotationStorage) {
		this.outputConsumer = outputConsumer;
		this.remapper = remapper;
		this.annotationStorage = annotationStorage;
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

		ClassReader reader = new ClassReader(content);
		ClassNode node = new ClassNode();
		ClassAccessWidenings accessWidenings = new ClassAccessWidenings();

		AtomicReference<EventBusSubscriber> eventBusSubscriber = new AtomicReference<>();

		Consumer<String> modConsumer = classModId -> {
			LOGGER.trace("Found @Mod annotation at %s (id: %s)", name, classModId);
			modInfo.add(new AbstractMap.SimpleImmutableEntry<>(classModId, name));
		};

		AnnotationProcessor scanner = new AnnotationProcessor(node, modConsumer, annotationStorage);
		ObjectHolderRewriter objectHolderScanner = new ObjectHolderRewriter(scanner);
		EventHandlerRewriter eventHandlerRewriter = new EventHandlerRewriter(objectHolderScanner, eventBusSubscriber::set);
		ItemGroupTransformer itemGroupTransformer = new ItemGroupTransformer(eventHandlerRewriter);
		BlockSettingsTransformer blockSettingsTransformer = new BlockSettingsTransformer(itemGroupTransformer);
		BiomeLayersTransformer biomeLayersTransformer = new BiomeLayersTransformer(blockSettingsTransformer);
		ExtensibleEnumTransformer extensibleEnumTransformer = new ExtensibleEnumTransformer(biomeLayersTransformer);
		EventSubclassTransformer eventSubclassTransformer = new EventSubclassTransformer(extensibleEnumTransformer);
		LevelGeneratorTypeTransformer levelGeneratorTypeTransformer = new LevelGeneratorTypeTransformer(eventSubclassTransformer);
		CapabilityInjectRewriter capabilityInjectRewriter = new CapabilityInjectRewriter(levelGeneratorTypeTransformer);
		StringConstantRemapper stringRemapperTransformer = new StringConstantRemapper(capabilityInjectRewriter, remapper.getNaiveRemapper());

		reader.accept(stringRemapperTransformer, ClassReader.EXPAND_FRAMES);

		// Post processing & state tracking
		SubscribingClass subscribingClass = eventHandlerRewriter.asSubscribingClass();

		if (subscribingClass.hasInstanceSubscribers() || subscribingClass.hasStaticSubscribers()) {
			accessWidenings.makeClassPublic();

			subscribingClasses.add(subscribingClass);
		}

		if (eventBusSubscriber.get() != null) {
			if (!subscribingClass.hasStaticSubscribers()) {
				Patchwork.LOGGER.warn("Ignoring the @EventBusSubscriber annotation on %s because it has no static methods with @SubscribeEvent", name);
			} else {
				EventBusSubscriber subscriber = eventBusSubscriber.get();

				eventBusSubscribers.add(new AbstractMap.SimpleImmutableEntry<>(name, subscriber));
			}
		}

		if (!objectHolderScanner.getObjectHolders().isEmpty()) {
			accessWidenings.makeClassPublic();

			for (ObjectHolder holder : objectHolderScanner.getObjectHolders()) {
				accessWidenings.definalizeField(holder.getField(), holder.getDescriptor());
			}

			objectHolderClasses.add(name);
		}

		if (!capabilityInjectRewriter.getInjects().isEmpty()) {
			accessWidenings.makeClassPublic();
			capabilityInjectClasses.add(name);
		}

		// Writing
		ClassWriter writer = new ClassWriter(0);

		accessWidenings.apply(node);
		node.accept(writer);

		outputConsumer.accept(name, writer.toByteArray());

		// Inheritance tracking
		List<String> supers = new ArrayList<>();
		supers.add(reader.getSuperName());
		supers.addAll(Arrays.asList(reader.getInterfaces()));
		checker.onClassScanned(name, eventHandlerRewriter.getSubscribeEvents(), supers);
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

		objectHolderClasses.clear();
		subscribingClasses.clear();
		eventBusSubscribers.clear();

		modInfo.forEach(entry -> {
			if (entry.getKey().equals(primaryId)) {
				return;
			}

			generateInitializer(entry.getKey(), entry.getValue(), entrypoints);
		});

		checker.check();

		return primaryId;
	}

	private void generateInitializer(String id, String clazz, Consumer<String> entrypoints) {
		ClassWriter initializerWriter = new ClassWriter(0);
		String initializerName = "patchwork_generated/" + clazz + "Initializer";

		List<Map.Entry<String, Consumer<MethodVisitor>>> initializerSteps = new ArrayList<>();

		// TODO: Need to check if the base classes are annotated with @OnlyIn / @Environment

		initializerSteps.add(new AbstractMap.SimpleImmutableEntry<>("registerEventRegistrars", new RegisterEventRegistrars(subscribingClasses)));
		// TODO: This should probably be first? How do we do event registrars without classloading the target class?
		initializerSteps.add(new AbstractMap.SimpleImmutableEntry<>("constructTargetMod", new ConstructTargetMod(clazz)));
		initializerSteps.add(new AbstractMap.SimpleImmutableEntry<>("registerAutomaticSubscribers", new RegisterAutomaticSubscribers(eventBusSubscribers)));
		initializerSteps.add(new AbstractMap.SimpleImmutableEntry<>("registerObjectHolders", new RegisterObjectHolders(objectHolderClasses)));
		initializerSteps.add(new AbstractMap.SimpleImmutableEntry<>("registerCapabilityInjects", new RegisterCapabilityInjects(capabilityInjectClasses)));

		ForgeInitializerGenerator.generate(initializerName, id, initializerSteps, initializerWriter);

		entrypoints.accept(initializerName.replace('/', '.'));
		outputConsumer.accept(initializerName, initializerWriter.toByteArray());
	}
}
