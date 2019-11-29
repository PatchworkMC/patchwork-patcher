package net.coderbot.patchwork;

import net.coderbot.patchwork.access.AccessTransformation;
import net.coderbot.patchwork.access.ClassAccessTransformations;
import net.coderbot.patchwork.access.ModAccessTransformer;
import net.coderbot.patchwork.annotation.AnnotationProcessor;
import net.coderbot.patchwork.at.AccessorInterfaceGenerator;
import net.coderbot.patchwork.at.ModGutter;
import net.coderbot.patchwork.event.EventBusSubscriber;
import net.coderbot.patchwork.event.EventHandlerScanner;
import net.coderbot.patchwork.event.SubscribeEvent;
import net.coderbot.patchwork.event.generator.InstanceEventRegistrarGenerator;
import net.coderbot.patchwork.event.generator.StaticEventRegistrarGenerator;
import net.coderbot.patchwork.event.generator.SubscribeEventGenerator;
import net.coderbot.patchwork.manifest.chocoasm.SilkyGenerator;
import net.coderbot.patchwork.manifest.converter.ModManifestConverter;
import net.coderbot.patchwork.manifest.forge.AccessTransformerList;
import net.coderbot.patchwork.manifest.forge.ModManifest;
import net.coderbot.patchwork.mapping.*;
import net.coderbot.patchwork.objectholder.ForgeInitializerGenerator;
import net.coderbot.patchwork.objectholder.ObjectHolder;
import net.coderbot.patchwork.objectholder.ObjectHolderGenerator;
import net.coderbot.patchwork.objectholder.ObjectHolderScanner;
import net.coderbot.patchwork.patch.BlockSettingsTransformer;
import net.coderbot.patchwork.patch.ItemGroupTransformer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.tinyremapper.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class Patchwork {
	private static TinyRemapper voldeToOfficialTiny;
	private static TinyRemapper officialToIntermediaryTiny;
	public static void main(String[] args) throws Exception {
		Mappings intermediary = MappingsProvider.readTinyMappings(
				new FileInputStream(new File("data/mappings/intermediary-1.14.4.tiny")));
		List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(
				new FileInputStream(new File("data/mappings/voldemap-1.14.4.tsrg")));

		IMappingProvider intermediaryMappings = TinyUtils.createTinyMappingProvider(
				Paths.get("data/mappings/intermediary-1.14.4.tiny"), "official", "intermediary");
		TsrgMappings mappings = new TsrgMappings(classes, intermediary, "official");
		InvertedTsrgMappings voldeToOfficial = new InvertedTsrgMappings(mappings);
		String tiny = mappings.writeTiny("srg");

		Files.write(Paths.get("data/mappings/voldemap-1.14.4.tiny"),
				tiny.getBytes(StandardCharsets.UTF_8));

		Files.createDirectories(Paths.get("input"));
		Files.createDirectories(Paths.get("temp"));
		Files.createDirectories(Paths.get("output"));
		voldeToOfficialTiny = TinyRemapper.newRemapper()
									  .withMappings(voldeToOfficial)
									  .rebuildSourceFilenames(true)
									  .ignoreFieldDesc(true)
									  .build();
		officialToIntermediaryTiny = TinyRemapper.newRemapper()
											 .withMappings(intermediaryMappings)
											 .rebuildSourceFilenames(true)
											 .ignoreFieldDesc(true)
											 .build();
		voldeToOfficialTiny.readClassPath(Paths.get("data/1.14.4+srg.jar"));
		officialToIntermediaryTiny.readClassPath(Paths.get("data/1.14.4+official.jar"));
		// This takes a long time, so we skip it.
		//
		// System.out.println("Remapping Minecraft (official -> srg)");
		// remap(mappings, Paths.get("data/1.14.4+official.jar"), Paths.get("data/1.14.4+srg.jar"));

		Files.walk(Paths.get("input")).forEach(file -> {
			if(!file.toString().endsWith(".jar")) {
				return;
			}

			String modName = file.toString().replaceAll("input/", "").replaceAll(".jar", "");

			System.out.println("=== Transforming " + modName + " ===");

			try {
				transformMod(modName);
			} catch(Exception e) {
				System.err.println("Transformation failed, going on to next mod: ");

				e.printStackTrace();
			}
		});
		voldeToOfficialTiny.finish();
		officialToIntermediaryTiny.finish();
	}

	public static void transformMod(String mod) throws Exception {
		System.out.println("Remapping " + mod + " (srg -> official)");

		remap(voldeToOfficialTiny,
				Paths.get("input/" + mod + ".jar"),
				Paths.get("temp/" + mod + "+official.jar"),
				Paths.get("data/1.14.4+srg.jar"));

		System.out.println("Remapping " + mod + " (official -> intermediary)");
		remap(officialToIntermediaryTiny,
				Paths.get("temp/" + mod + "+official.jar"),
				Paths.get("temp/" + mod + "+intermediary.jar"),
				Paths.get("data/1.14.4+official.jar"));

		// Now scan for annotations, strip them, and replace them with pointers.

		Path input = Paths.get("temp/" + mod + "+intermediary.jar");
		Path output = Paths.get("output/" + mod + ".jar");

		URI uri = new URI("jar:" + input.toUri().toString());
		FileSystem fs = null;
		boolean shouldClose = false;

		try {
			fs = FileSystems.getFileSystem(uri);
		} catch(FileSystemNotFoundException e) {
		}

		if(fs == null) {
			fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
			shouldClose = true;
		}

		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();
		outputConsumer.addNonClassFiles(input);

		List<Map.Entry<String, ObjectHolder>> generatedObjectHolderEntries =
				new ArrayList<>(); // shimName -> ObjectHolder
		List<Map.Entry<String, String>> staticEventRegistrars =
				new ArrayList<>(); // shimName -> baseName
		List<Map.Entry<String, String>> instanceEventRegistrars =
				new ArrayList<>(); // shimName -> baseName
		List<Map.Entry<String, EventBusSubscriber>> eventBusSubscribers =
				new ArrayList<>(); // basename -> EventBusSubscriber

		HashMap<String, String> modInfo = new HashMap<>();
		// Devoldify the accesstransformer

		Path accessTransformer = fs.getPath("/META-INF/accesstransformer.cfg");
		AccessTransformerList accessTransformers = AccessTransformerList.parse(accessTransformer);
		accessTransformers.remap(voldeToOfficialTiny.getRemapper())
				.remap(officialToIntermediaryTiny.getRemapper());
		List<ModGutter.Meta> metas = new ArrayList<>();
		Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				String name = file.toString();

				if(name.endsWith(".class")) {
					String baseName = name.substring(0, name.length() - ".class".length());
					byte[] content = Files.readAllBytes(file);

					ClassReader reader = new ClassReader(content);
					ClassNode node = new ClassNode();

					List<ObjectHolder> objectHolders = new ArrayList<>();
					List<SubscribeEvent> subscribeEvents = new ArrayList<>();

					ClassAccessTransformations accessTransformations =
							new ClassAccessTransformations();
					Consumer<String> modConsumer = classModId -> {
						System.out.println(
								"Class " + baseName + " has @Mod annotation: " + classModId);
						modInfo.put(classModId, baseName);
					};

					AnnotationProcessor scanner = new AnnotationProcessor(node, modConsumer);
					ObjectHolderScanner objectHolderScanner =
							new ObjectHolderScanner(scanner, holder -> {
								objectHolders.add(holder);

								accessTransformations.addFieldTransformation(
										holder.getField(), AccessTransformation.DEFINALIZE);
							});

					EventHandlerScanner eventHandlerScanner = new EventHandlerScanner(
							objectHolderScanner,
							subscriber
							-> {
								// System.out.println(subscriber);

								eventBusSubscribers.add(new AbstractMap.SimpleImmutableEntry<>(
										baseName, subscriber));
							},
							subscribeEvent -> {
								// System.out.println(subscribeEvent);

								subscribeEvents.add(subscribeEvent);

								accessTransformations.setClassTransformation(
										AccessTransformation.MAKE_PUBLIC);

								accessTransformations.addMethodTransformation(
										subscribeEvent.getMethod(),
										subscribeEvent.getMethodDescriptor(),
										AccessTransformation.MAKE_PUBLIC);
							});

					ItemGroupTransformer itemGroupTransformer =
							new ItemGroupTransformer(eventHandlerScanner);
					BlockSettingsTransformer blockSettingsTransformer =
							new BlockSettingsTransformer(itemGroupTransformer);
					reader.accept(blockSettingsTransformer, ClassReader.EXPAND_FRAMES);
					ClassWriter writer = new ClassWriter(0);
					ModAccessTransformer modAccessTransformer =
							new ModAccessTransformer(writer, accessTransformations);
					ModGutter modGutter =
							new ModGutter(accessTransformers.getEntries(), modAccessTransformer);
					node.accept(modGutter);
					metas.addAll(modGutter.metas);
					objectHolders.forEach(entry -> {
						ClassWriter shimWriter = new ClassWriter(0);
						String shimName =
								ObjectHolderGenerator.generate(baseName, entry, shimWriter);

						generatedObjectHolderEntries.add(
								new AbstractMap.SimpleImmutableEntry<>(shimName, entry));

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

						String shimName =
								SubscribeEventGenerator.generate(baseName, entry, shimWriter);

						if(subscribeEventStaticShims.containsKey(shimName) ||
								subscribeEventInstanceShims.containsKey(shimName)) {
							throw new UnsupportedOperationException(
									"FIXME: Two @SubscribeEvent shims have the same name! This should be handled by Patchwork, it's a bug!");
						}

						if((entry.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
							subscribeEventStaticShims.put(shimName, entry);
						} else {
							subscribeEventInstanceShims.put(shimName, entry);
						}

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());
					});

					if(!subscribeEventStaticShims.isEmpty()) {
						ClassWriter shimWriter = new ClassWriter(0);
						String shimName = StaticEventRegistrarGenerator.generate(
								baseName, subscribeEventStaticShims.entrySet(), shimWriter);

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());

						staticEventRegistrars.add(
								new AbstractMap.SimpleImmutableEntry<>(shimName, baseName));
					}

					if(!subscribeEventInstanceShims.isEmpty()) {
						ClassWriter shimWriter = new ClassWriter(0);
						String shimName = InstanceEventRegistrarGenerator.generate(
								baseName, subscribeEventInstanceShims.entrySet(), shimWriter);

						outputConsumer.accept("/" + shimName, shimWriter.toByteArray());

						instanceEventRegistrars.add(
								new AbstractMap.SimpleImmutableEntry<>(shimName, baseName));
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
			ForgeInitializerGenerator.generate(modInfo.get(id),
					initializerName,
					id,
					staticEventRegistrars,
					instanceEventRegistrars,
					eventBusSubscribers,
					generatedObjectHolderEntries,
					initializerWriter);
			entrypoint.add(initializerName.replace('/', '.'));
			outputConsumer.accept("/" + initializerName, initializerWriter.toByteArray());
		});

		metas.forEach((m) -> {
			ClassWriter accessTransformerWriter = new ClassWriter(0);
			AccessorInterfaceGenerator.generate(mods.get(0).getAsJsonPrimitive("id").getAsString(), m, accessTransformerWriter);
			outputConsumer.accept("/patchwork_generated/" + mods.get(0).getAsJsonPrimitive("id").getAsString() + "/mixin/" + m.getName() +
										  "AccessorMixin",
					accessTransformerWriter.toByteArray());
		});
		outputConsumer.close();

		if(shouldClose) {
			fs.close();
		}

		uri = new URI("jar:" + output.toUri().toString());
		fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject primary = mods.get(0);

		entrypoints.add("patchwork", entrypoint);
		primary.add("entrypoints", entrypoints);

		JsonArray jarsArray = new JsonArray();
		mods.forEach(m -> {
			if(m != primary) {
				JsonObject file = new JsonObject();
				file.addProperty("file",
						"META-INF/jars/" + m.getAsJsonPrimitive("id").getAsString() + ".jar");
				jarsArray.add(file);
			}
		});

		primary.add("jars", jarsArray);
		String json = gson.toJson(primary);

		Path fabricModJson = fs.getPath("/fabric.mod.json");

		try {
			Files.delete(fabricModJson);
		} catch(IOException ignored) {
		}

		Files.write(fabricModJson, json.getBytes(StandardCharsets.UTF_8));
		Files.write(
				fs.getPath("/silky.at"), SilkyGenerator.generate(accessTransformers).getBytes());
		try {
			Files.createDirectory(fs.getPath("/META-INF/jars/"));
		} catch(IOException ignored) {
		}

		for(JsonObject entry : mods) {
			String modid = entry.getAsJsonPrimitive("id").getAsString();

			if(entry == primary) {
				// Don't write the primary jar as a jar-in-jar!
				continue;
			}

			ByteArrayOutputStream jar = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(jar);

			zip.putNextEntry(new ZipEntry("/fabric.mod.json"));
			zip.write(entry.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			Files.write(fs.getPath("/META-INF/jars/" + modid + ".jar"),
					jar.toByteArray(),
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);

			jar.close();
		}

		Files.delete(manifestPath);
		Files.delete(fs.getPath("pack.mcmeta"));
		// close everything

		fs.close();

		// Late entrypoints
		// https://github.com/CottonMC/Cotton/blob/master/modules/cotton-datapack/src/main/java/io/github/cottonmc/cotton/datapack/mixins/MixinCottonInitializerServer.java
	}
	private static void remap(TinyRemapper remapper, Path input, Path output, Path... classpath)
			throws IOException {
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
