package com.patchworkmc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

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
import com.patchworkmc.logging.LogLevel;
import com.patchworkmc.logging.Logger;
import com.patchworkmc.logging.writer.StreamWriter;
import com.patchworkmc.manifest.converter.ModManifestConverter;
import com.patchworkmc.manifest.forge.ModManifest;
import com.patchworkmc.mapping.BridgedMappings;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.TinyWriter;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import com.patchworkmc.objectholder.ForgeInitializerGenerator;
import com.patchworkmc.objectholder.ObjectHolder;
import com.patchworkmc.objectholder.ObjectHolderGenerator;
import com.patchworkmc.objectholder.ObjectHolderScanner;
import com.patchworkmc.patch.BlockSettingsTransformer;
import com.patchworkmc.patch.ItemGroupTransformer;

public class Patchwork {
	public static final Logger LOGGER;
	private static String version = "1.14.4";

	static {
		LOGGER = Logger.getInstance();
		LOGGER.setWriter(new StreamWriter(true, System.out, System.err), LogLevel.TRACE);
	}

	public static void main(String[] args) throws Exception {
		File current = new File(System.getProperty("user.dir"));
		Path currentPath = current.toPath();
		File voldemapTiny = new File(current, "data/mappings/voldemap-" + version + ".tiny");
		List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(new FileInputStream(new File(current, "data/mappings/voldemap-" + version + ".tsrg")));

		IMappingProvider intermediary = TinyUtils.createTinyMappingProvider(currentPath.resolve("data/mappings/intermediary-" + version + ".tiny"), "official", "intermediary");
		TsrgMappings mappings = new TsrgMappings(classes, intermediary);

		if (!voldemapTiny.exists()) {
			TinyWriter tinyWriter = new TinyWriter("official", "srg");
			mappings.load(tinyWriter);
			String tiny = tinyWriter.toString();
			Files.write(voldemapTiny.toPath(), tiny.getBytes(StandardCharsets.UTF_8));
		}

		File voldemapBridged = new File(current, "data/mappings/voldemap-bridged-" + version + ".tiny");
		IMappingProvider bridged;

		if (!voldemapBridged.exists()) {
			System.out.println("Generating bridged (srg -> intermediary) tiny mappings");

			TinyWriter tinyWriter = new TinyWriter("srg", "intermediary");
			bridged = new BridgedMappings(mappings, intermediary);
			bridged.load(tinyWriter);

			Files.write(voldemapBridged.toPath(), tinyWriter.toString().getBytes(StandardCharsets.UTF_8));
		} else {
			System.out.println("Using cached bridged (srg -> intermediary) tiny mappings");

			bridged = TinyUtils.createTinyMappingProvider(voldemapBridged.toPath(), "srg", "intermediary");
		}

		Files.createDirectories(currentPath.resolve("input"));
		Files.createDirectories(currentPath.resolve("temp"));
		Files.createDirectories(currentPath.resolve("output"));

		Files.walk(currentPath.resolve("input")).forEach(file -> {
			if (!file.toString().endsWith(".jar")) {
				return;
			}

			String modName = file.getFileName().toString().replaceAll(".jar", "");

			LOGGER.info("=== Transforming " + modName + " ===");

			try {
				transformMod(currentPath, file, currentPath.resolve("output"), modName, bridged);
			} catch (Exception e) {
				LOGGER.error("Transformation failed, going on to next mod: ");

				e.printStackTrace();
			}
		});
	}

	public static void transformMod(Path currentPath, Path jarPath, Path outputRoot, String mod, IMappingProvider bridged)
					throws Exception {
		System.out.println("Remapping " + mod + " (TinyRemapper, srg -> intermediary)");
		remap(bridged, jarPath, currentPath.resolve("temp/" + mod + "+intermediary.jar"), currentPath.resolve("data/" + version + "-client+srg.jar"));

		// Now scan for annotations, strip them, and replace them with pointers.

		Path input = currentPath.resolve("temp/" + mod + "+intermediary.jar");
		Path output = outputRoot.resolve(mod + ".jar");

		URI uri = new URI("jar:" + input.toUri().toString());
		FileSystem fs = null;
		boolean shouldClose = false;

		try {
			fs = FileSystems.getFileSystem(uri);
		} catch (FileSystemNotFoundException e) {
			// ignored
		}

		if (fs == null) {
			fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
			shouldClose = true;
		}

		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();
		outputConsumer.addNonClassFiles(input);

		List<Map.Entry<String, ObjectHolder>> generatedObjectHolderEntries = new ArrayList<>(); // shimName -> ObjectHolder
		List<Map.Entry<String, String>> staticEventRegistrars = new ArrayList<>(); // shimName -> baseName
		List<Map.Entry<String, String>> instanceEventRegistrars = new ArrayList<>(); // shimName -> baseName
		List<Map.Entry<String, EventBusSubscriber>> eventBusSubscribers = new ArrayList<>(); // basename -> EventBusSubscriber

		HashMap<String, String> modInfo = new HashMap<>();

		Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.toString();

				if (name.endsWith(".class")) {
					String baseName = name.substring(0, name.length() - ".class".length());

					if (baseName.startsWith("/net/minecraft")) {
						throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Minecraft's package!");
					}

					if (baseName.startsWith("/java")) {
						throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Java's package!");
					}

					byte[] content = Files.readAllBytes(file);

					ClassReader reader = new ClassReader(content);
					ClassNode node = new ClassNode();

					List<ObjectHolder> objectHolders = new ArrayList<>();
					List<SubscribeEvent> subscribeEvents = new ArrayList<>();

					AccessTransformations accessTransformations = new AccessTransformations();

					Consumer<String> modConsumer = classModId -> {
						LOGGER.trace("Found @Mod annotation at " + baseName + " (id: " + classModId + ")");
						modInfo.put(classModId, baseName);
						// modName.set(baseName);
						// modId.set(classModId);
					};

					AnnotationProcessor scanner = new AnnotationProcessor(node, modConsumer);
					ObjectHolderScanner objectHolderScanner = new ObjectHolderScanner(scanner, holder -> {
						objectHolders.add(holder);

						accessTransformations.addFieldTransformation(holder.getField(), AccessTransformation.DEFINALIZE_MAKE_PUBLIC);
					});

					EventHandlerScanner eventHandlerScanner = new EventHandlerScanner(objectHolderScanner, subscriber -> {
						// LOGGER.info(subscriber);

						eventBusSubscribers.add(new AbstractMap.SimpleImmutableEntry<>(baseName, subscriber));
					}, subscribeEvent -> {
						// LOGGER.info(subscribeEvent);

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
						String shimName = ObjectHolderGenerator.generate(baseName, entry, shimWriter);

						generatedObjectHolderEntries.add(new AbstractMap.SimpleImmutableEntry<>(shimName, entry));

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());
					});

					HashMap<String, SubscribeEvent> subscribeEventStaticShims = new HashMap<>();
					HashMap<String, SubscribeEvent> subscribeEventInstanceShims = new HashMap<>();

					subscribeEvents.forEach(entry -> {
						ClassWriter shimWriter = new ClassWriter(0);
						String shimName = SubscribeEventGenerator.generate(baseName, entry, shimWriter);

						if (subscribeEventStaticShims.containsKey(shimName) || subscribeEventInstanceShims.containsKey(shimName)) {
							throw new UnsupportedOperationException("FIXME: Two @SubscribeEvent shims have the same name! This should be handled by Patchwork, it's a bug!");
						}

						if ((entry.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
							subscribeEventStaticShims.put(shimName, entry);
						} else {
							subscribeEventInstanceShims.put(shimName, entry);
						}

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());
					});

					if (!subscribeEventStaticShims.isEmpty()) {
						ClassWriter shimWriter = new ClassWriter(0);
						String shimName = StaticEventRegistrarGenerator.generate(baseName, subscribeEventStaticShims.entrySet(), shimWriter);

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());

						staticEventRegistrars.add(new AbstractMap.SimpleImmutableEntry<>(shimName, baseName));
					}

					if (!subscribeEventInstanceShims.isEmpty()) {
						ClassWriter shimWriter = new ClassWriter(0);
						String shimName = InstanceEventRegistrarGenerator.generate(baseName, subscribeEventInstanceShims.entrySet(), shimWriter);

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());

						instanceEventRegistrars.add(new AbstractMap.SimpleImmutableEntry<>(shimName, baseName));
					}

					outputConsumer.accept(baseName, writer.toByteArray());
				}

				return FileVisitResult.CONTINUE;
			}
		});

		Path manifestPath = fs.getPath("/META-INF/mods.toml");

		FileConfig toml = FileConfig.of(manifestPath);
		toml.load();

		Map<String, Object> map = toml.valueMap();
		LOGGER.trace("\nRaw mod toml:");
		map.forEach((s, o) -> {
			LOGGER.trace("  " + s + ": " + o);
		});
		LOGGER.trace("");

		ModManifest manifest = ModManifest.parse(map);

		// TODO: register instance event registrars
		List<JsonObject> mods = ModManifestConverter.convertToFabric(manifest);

		JsonObject entrypoints = new JsonObject();
		JsonArray entrypoint = new JsonArray();
		mods.forEach(m -> {
			String id = m.getAsJsonPrimitive("id").getAsString();
			ClassWriter initializerWriter = new ClassWriter(0);
			String initializerName = "patchwork_generated" + modInfo.get(id) + "Initializer";
			ForgeInitializerGenerator.generate(modInfo.get(id), initializerName, id, staticEventRegistrars, instanceEventRegistrars, eventBusSubscribers, generatedObjectHolderEntries, initializerWriter);
			entrypoint.add(initializerName.replace('/', '.'));
			outputConsumer.accept("/" + initializerName, initializerWriter.toByteArray());
		});

		outputConsumer.close();

		if (shouldClose) {
			fs.close();
		}

		uri = new URI("jar:" + output.toUri().toString());
		fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

		// LOGGER.info("Parsed: " + manifest);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject primary = mods.get(0);

		entrypoints.add("patchwork", entrypoint);
		primary.add("entrypoints", entrypoints);

		JsonArray jarsArray = new JsonArray();
		mods.forEach(m -> {
			if (m != primary) {
				JsonObject file = new JsonObject();
				file.addProperty("file", "META-INF/jars/" + m.getAsJsonPrimitive("id").getAsString() + ".jar");
				jarsArray.add(file);
			}
		});

		primary.add("jars", jarsArray);
		String json = gson.toJson(primary);

		Path fabricModJson = fs.getPath("/fabric.mod.json");

		try {
			Files.delete(fabricModJson);
		} catch (IOException ignored) {
			// ignored
		}

		Files.write(fabricModJson, json.getBytes(StandardCharsets.UTF_8));

		LOGGER.trace("fabric.mod.json: " + json);

		try {
			Files.createDirectory(fs.getPath("/META-INF/jars/"));
		} catch (IOException ignored) {
			// ignored
		}

		for (JsonObject entry : mods) {
			String modid = entry.getAsJsonPrimitive("id").getAsString();

			if (entry == primary) {
				// Don't write the primary jar as a jar-in-jar!

				continue;
			}

			ByteArrayOutputStream jar = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(jar);

			zip.putNextEntry(new ZipEntry("/fabric.mod.json"));
			zip.write(entry.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			Files.write(fs.getPath("/META-INF/jars/" + modid + ".jar"), jar.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

			jar.close();
		}

		Files.delete(manifestPath);
		Files.delete(fs.getPath("pack.mcmeta"));

		fs.close();

		// Late entrypoints
		// https://github.com/CottonMC/Cotton/blob/master/modules/cotton-datapack/src/main/java/io/github/cottonmc/cotton/datapack/mixins/MixinCottonInitializerServer.java
	}

	public static void remap(IMappingProvider mappings, Path input, Path output, Path... classpath)
					throws IOException {
		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(mappings).rebuildSourceFilenames(true).build();

		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();

		try {
			outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);

			remapper.readClassPath(classpath);
			remapper.readInputs(input);
			remapper.apply(outputConsumer);
		} finally {
			outputConsumer.close();
			remapper.finish();
		}
	}
}
