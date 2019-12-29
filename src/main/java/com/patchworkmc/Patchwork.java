package com.patchworkmc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import com.patchworkmc.logging.LogLevel;
import com.patchworkmc.logging.Logger;
import com.patchworkmc.logging.writer.StreamWriter;
import com.patchworkmc.manifest.converter.ModManifestConverter;
import com.patchworkmc.manifest.mod.ManifestParseException;
import com.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.mapping.BridgedMappings;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.TinyWriter;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import com.patchworkmc.transformer.PatchworkTransformer;

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
			LOGGER.trace("Generating bridged (srg -> intermediary) tiny mappings");

			TinyWriter tinyWriter = new TinyWriter("srg", "intermediary");
			bridged = new BridgedMappings(mappings, intermediary);
			bridged.load(tinyWriter);

			Files.write(voldemapBridged.toPath(), tinyWriter.toString().getBytes(StandardCharsets.UTF_8));
		} else {
			LOGGER.trace("Using cached bridged (srg -> intermediary) tiny mappings");

			bridged = TinyUtils.createTinyMappingProvider(voldemapBridged.toPath(), "srg", "intermediary");
		}

		Files.createDirectories(currentPath.resolve("input"));
		Files.createDirectories(currentPath.resolve("output"));
		Stream<Path> inputWalk = Files.walk(currentPath.resolve("input"));
		inputWalk.forEach(file -> {
			if (!file.toString().endsWith(".jar")) {
				return;
			}

			String modName = file.getFileName().toString().replaceAll(".jar", "");

			LOGGER.info("=== Transforming " + modName + " ===");

			try {
				transformMod(currentPath, file, currentPath.resolve("output"), modName, bridged);
			} catch (Exception e) {
				LOGGER.error("Transformation failed, going on to next mod: ");

				LOGGER.thrown(LogLevel.ERROR, e);
			}
		});
		inputWalk.close();
	}

	public static void transformMod(Path currentPath, Path jarPath, Path outputRoot, String mod, IMappingProvider mappings)
		throws IOException, URISyntaxException, ManifestParseException {
		LOGGER.info("Remapping and patching %s (TinyRemapper, srg -> intermediary)", mod);
		Path output = outputRoot.resolve(mod + ".jar");

		TinyRemapper remapper = null;

		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();
		PatchworkTransformer transformer = new PatchworkTransformer(outputConsumer);
		JsonArray patchworkEntrypoints = new JsonArray();

		try {
			remapper = remap(mappings, jarPath, transformer, currentPath.resolve("data/" + version + "-client+srg.jar"));

			// Write the ForgeInitializer
			transformer.finish(patchworkEntrypoints::add);
			outputConsumer.addNonClassFiles(jarPath, NonClassCopyMode.FIX_META_INF, remapper);
		} finally {
			if (remapper != null) {
				remapper.finish();
			}

			outputConsumer.close();
		}

		// Done remapping/patching

		LOGGER.info("Rewriting mod metadata for %s", mod);

		URI uri = new URI("jar:" + output.toUri().toString());
		FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

		Path manifestPath = fs.getPath("/META-INF/mods.toml");

		FileConfig toml = FileConfig.of(manifestPath);
		toml.load();

		Map<String, Object> map = toml.valueMap();
		LOGGER.trace("\nRaw mod toml:");
		map.forEach((s, o) -> LOGGER.trace("  " + s + ": " + o));

		LOGGER.trace("Parsing and converting mods.toml to fabric.mod.json");

		ModManifest manifest = ModManifest.parse(map);

		if (!manifest.getModLoader().equals("javafml")) {
			LOGGER.error("Unsupported modloader %s", manifest.getModLoader());
		}

		List<JsonObject> mods = ModManifestConverter.convertToFabric(manifest);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject primary = mods.get(0);
		JsonObject entrypoints = new JsonObject();

		entrypoints.add("patchwork", patchworkEntrypoints);
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
		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();
		TinyRemapper remapper = null;

		try {
			remapper = remap(mappings, input, outputConsumer, classpath);
			outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);
		} finally {
			if (remapper != null) {
				remapper.finish();
			}

			outputConsumer.close();
		}
	}

	public static TinyRemapper remap(IMappingProvider mappings, Path input, BiConsumer<String, byte[]> consumer, Path... classpath) {
		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(mappings).rebuildSourceFilenames(true).build();

		remapper.readClassPath(classpath);
		remapper.readInputs(input);
		remapper.apply(consumer);

		return remapper;
	}
}
