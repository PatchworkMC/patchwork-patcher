package com.patchworkmc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import com.patchworkmc.manifest.converter.ModManifestConverter;
import com.patchworkmc.manifest.forge.ModManifest;
import com.patchworkmc.mapping.InvertedTsrgMappings;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import com.patchworkmc.objectholder.ForgeInitializerGenerator;
import com.patchworkmc.objectholder.ObjectHolder;
import com.patchworkmc.objectholder.ObjectHolderGenerator;
import com.patchworkmc.objectholder.ObjectHolderScanner;
import com.patchworkmc.patch.BlockSettingsTransformer;
import com.patchworkmc.patch.ItemGroupTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

public class Patchwork {
	private static String version = "1.14.4";

	public static void main(String[] args) throws Exception {
		File current = new File(System.getProperty("user.dir"));
		Path currentPath = current.toPath();
		Mappings intermediary = MappingsProvider.readTinyMappings(loadOrDownloadIntermediary(version, new File(current, "data/mappings")));
		File voldemapTiny = new File(current, "data/mappings/voldemap-" + version + ".tiny");
		List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(loadOrDownloadMCPConfig(version, new File(current, "data/mappings")));
		IMappingProvider intermediaryMappings = TinyUtils.createTinyMappingProvider(currentPath.resolve("data/mappings/intermediary-" + version + ".tiny"), "official", "intermediary");
		TsrgMappings mappings = new TsrgMappings(classes, intermediary, "official");

		if (!voldemapTiny.exists()) {
			String tiny = mappings.writeTiny("srg");
			Files.write(voldemapTiny.toPath(), tiny.getBytes(StandardCharsets.UTF_8));
		}

		Files.createDirectories(currentPath.resolve("input"));
		Files.createDirectories(currentPath.resolve("temp"));
		Files.createDirectories(currentPath.resolve("output"));

		{
			Path officialJar = currentPath.resolve("data/" + version + "+official.jar");
			Path srgJar = currentPath.resolve("data/" + version + "+srg.jar");

			if (!Files.exists(officialJar)) {
				System.out.println("Downloading Minecraft " + version + ".");
				Gson gson = new GsonBuilder().disableHtmlEscaping().create();
				JsonArray versions = gson.fromJson(new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()), JsonObject.class).get("versions").getAsJsonArray();
				Files.deleteIfExists(srgJar);

				for (JsonElement jsonElement : versions) {
					if (jsonElement.isJsonObject()) {
						JsonObject object = jsonElement.getAsJsonObject();
						String id = object.get("id").getAsJsonPrimitive().getAsString();

						if (id.equals(version)) {
							String versionUrl = object.get("url").getAsJsonPrimitive().getAsString();
							JsonObject versionMeta = gson.fromJson(new InputStreamReader(new URL(versionUrl).openStream()), JsonObject.class);
							String versionJarUrl = versionMeta.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsJsonPrimitive().getAsString();
							FileUtils.copyURLToFile(new URL(versionJarUrl), officialJar.toFile());
							break;
						}
					}
				}

				if (!Files.exists(officialJar)) {
					throw new IllegalStateException("Failed to download Minecraft " + version);
				}
			}

			if (!Files.exists(srgJar)) {
				System.out.println("Remapping Minecraft (official -> srg)");
				remap(mappings, officialJar, srgJar);
			}
		}

		Files.walk(currentPath.resolve("input")).forEach(file -> {
			if (!file.toString().endsWith(".jar")) {
				return;
			}

			String modName = file.getFileName().toString().replaceAll(".jar", "");

			System.out.println("=== Transforming " + modName + " ===");

			try {
				transformMod(currentPath, modName, mappings, intermediaryMappings);
			} catch (Exception e) {
				System.err.println("Transformation failed, going on to next mod: ");

				e.printStackTrace();
			}
		});
	}

	private static InputStream loadOrDownloadMCPConfig(String version, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "voldemap-" + version + ".tsrg");

		if (!file.exists()) {
			System.out.println("Downloading MCP Config for " + version + ".");
			InputStream stream = new URL("http://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/" + version + "/mcp_config-" + version + ".zip").openStream();
			ZipInputStream zipInputStream = new ZipInputStream(stream);

			while (true) {
				ZipEntry nextEntry = zipInputStream.getNextEntry();
				if (nextEntry == null) break;

				if (!nextEntry.isDirectory() && nextEntry.getName().endsWith("/joined.tsrg")) {
					FileWriter writer = new FileWriter(file, false);
					IOUtils.copy(zipInputStream, writer, Charset.defaultCharset());
					writer.close();
					break;
				}
			}
		}

		return new FileInputStream(file);
	}

	private static InputStream loadOrDownloadIntermediary(String version, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "intermediary-" + version + ".tiny");

		if (!file.exists()) {
			System.out.println("Downloading Intermediary for " + version + ".");
			InputStream stream = new URL("https://maven.fabricmc.net/net/fabricmc/intermediary/" + version + "/intermediary-" + version + ".jar").openStream();
			ZipInputStream zipInputStream = new ZipInputStream(stream);

			while (true) {
				ZipEntry nextEntry = zipInputStream.getNextEntry();

				if (nextEntry == null) {
					break;
				}

				if (!nextEntry.isDirectory() && nextEntry.getName().endsWith("/mappings.tiny")) {
					FileWriter writer = new FileWriter(file, false);
					IOUtils.copy(zipInputStream, writer, Charset.defaultCharset());
					writer.close();
					break;
				}
			}
		}

		return new FileInputStream(file);
	}

	public static void transformMod(Path currentPath, String mod, TsrgMappings mappings, IMappingProvider intermediaryMappings)
		throws Exception {
		System.out.println("Remapping " + mod + " (srg -> official)");
		remap(new InvertedTsrgMappings(mappings), currentPath.resolve("input/" + mod + ".jar"), currentPath.resolve("temp/" + mod + "+official.jar"), currentPath.resolve("data/" + version + "+srg.jar"));

		System.out.println("Remapping " + mod + " (official -> intermediary)");
		remap(intermediaryMappings, currentPath.resolve("temp/" + mod + "+official.jar"), currentPath.resolve("temp/" + mod + "+intermediary.jar"), currentPath.resolve("data/" + version + "+official.jar"));

		// Now scan for annotations, strip them, and replace them with pointers.

		Path input = currentPath.resolve("temp/" + mod + "+intermediary.jar");
		Path output = currentPath.resolve("output/" + mod + ".jar");

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
					byte[] content = Files.readAllBytes(file);

					ClassReader reader = new ClassReader(content);
					ClassNode node = new ClassNode();

					List<ObjectHolder> objectHolders = new ArrayList<>();
					List<SubscribeEvent> subscribeEvents = new ArrayList<>();

					AccessTransformations accessTransformations = new AccessTransformations();

					Consumer<String> modConsumer = classModId -> {
						System.out.println("Class " + baseName + " has @Mod annotation: " + classModId);
						modInfo.put(classModId, baseName);
						// modName.set(baseName);
						// modId.set(classModId);
					};

					AnnotationProcessor scanner = new AnnotationProcessor(node, modConsumer);
					ObjectHolderScanner objectHolderScanner = new ObjectHolderScanner(scanner, holder -> {
						objectHolders.add(holder);

						accessTransformations.addFieldTransformation(holder.getField(), AccessTransformation.DEFINALIZE);
					});

					EventHandlerScanner eventHandlerScanner = new EventHandlerScanner(objectHolderScanner, subscriber -> {
						// System.out.println(subscriber);

						eventBusSubscribers.add(new AbstractMap.SimpleImmutableEntry<>(baseName, subscriber));
					}, subscribeEvent -> {
						// System.out.println(subscribeEvent);

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

						/*if((entry.getAccess() & Opcodes.ACC_STATIC) == 0) {
							System.err.println(
									"Instance subscribe events are not supported yet, skipping: " +
									baseName + "::" + entry.getMethod());

							return;
						}*/

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

		System.out.println("Raw: " + map);

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

		// System.out.println("Parsed: " + manifest);

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

		// System.out.println(json);
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

	private static void remap(IMappingProvider mappings, Path input, Path output, Path... classpath)
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
