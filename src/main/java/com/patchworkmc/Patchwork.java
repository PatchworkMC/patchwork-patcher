package com.patchworkmc;

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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.patchworkmc.manifest.accesstransformer.AccessTransformerList;
import com.patchworkmc.manifest.converter.ModManifestConverter;
import com.patchworkmc.manifest.mod.ManifestParseException;
import com.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.mapping.BridgedMappings;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.TinyWriter;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import com.patchworkmc.mapping.remapper.ManifestRemapper;
import com.patchworkmc.mapping.remapper.NaiveRemapper;
import com.patchworkmc.transformer.PatchworkTransformer;

public class Patchwork {
	public static final Logger LOGGER;
	private static String version = "1.14.4";

	private byte[] patchworkGreyscaleIcon;

	private Path inputDir, outputDir, dataDir, tempDir;
	private IMappingProvider primaryMappings;
	private List<IMappingProvider> devMappings;
	private NaiveRemapper naiveRemapper;
	private ManifestRemapper manifestRemapper;
	private boolean closed = false;

	static {
		// TODO: With the new logger from application-core, this is
		// 		 a little problem, since it does not follow the concept of
		//		 component sub loggers (see Logger#sub)
		LOGGER = new Logger("Patchwork");
		LOGGER.setWriter(new StreamWriter(true, System.out, System.err), LogLevel.TRACE);
	}

	public Patchwork(Path inputDir, Path outputDir, Path dataDir, Path tempDir, IMappingProvider primaryMappings, List<IMappingProvider> devMappings) {
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.dataDir = dataDir;
		this.tempDir = tempDir;
		this.primaryMappings = primaryMappings;
		this.devMappings = devMappings;

		try {
			this.patchworkGreyscaleIcon = Files.readAllBytes(Paths.get(Patchwork.class.getResource("/patchwork-icon-greyscale.png").toURI()));
		} catch (IOException | URISyntaxException ex) {
			LOGGER.thrown(LogLevel.FATAL, ex);
		}

		this.naiveRemapper = new NaiveRemapper(primaryMappings);
		this.manifestRemapper = new ManifestRemapper(primaryMappings);
	}

	public int patchAndFinish() throws IOException {
		if (this.closed) {
			throw new IllegalStateException("Cannot begin patching: Already patched all mods!");
		}

		int count = 0;

		try (Stream<Path> inputFilesStream = Files.walk(inputDir).filter(file -> file.toString().endsWith(".jar"))) {
			List<Path> inputFiles = inputFilesStream.collect(Collectors.toList());

			for (Path jarPath : inputFiles) {
				try {
					transformMod(jarPath);
					count++;

					generateDevJar(inputDir.relativize(jarPath));
				} catch (Exception ex) {
					LOGGER.thrown(LogLevel.ERROR, ex);
				}
			}
		}

		finish();
		return count;
	}

	private void transformMod(Path jarPath) throws IOException, URISyntaxException, ManifestParseException {
		String mod = jarPath.getFileName().toString().split("\\.jar")[0];
		// Load metadata
		LOGGER.trace("Loading and parsing metadata");
		URI inputJar = new URI("jar:" + jarPath.toUri().toString());

		FileConfig toml;
		AccessTransformerList list;

		try (FileSystem fs = FileSystems.newFileSystem(inputJar, Collections.emptyMap())) {
			Path manifestPath = fs.getPath("/META-INF/mods.toml");
			toml = FileConfig.of(manifestPath);
			toml.load();
			list = AccessTransformerList.parse(fs.getPath("/META-INF/access_transformer.cfg"));
		}

		Map<String, Object> map = toml.valueMap();
		LOGGER.trace("\nRaw mod toml:");
		map.forEach((s, o) -> LOGGER.trace("  " + s + ": " + o));

		ModManifest manifest = ModManifest.parse(map);

		if (!manifest.getModLoader().equals("javafml")) {
			LOGGER.error("Unsupported modloader %s", manifest.getModLoader());
		}

		LOGGER.trace("Remapping access transformers");

		list.remap(manifestRemapper);

		LOGGER.info("Remapping and patching %s (TinyRemapper, srg -> intermediary)", mod);
		Path output = outputDir.resolve(mod + ".jar");
		// Delete old patched jar
		Files.deleteIfExists(output);
		TinyRemapper remapper = null;

		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();
		PatchworkTransformer transformer = new PatchworkTransformer(outputConsumer, naiveRemapper);
		JsonArray patchworkEntrypoints = new JsonArray();

		try {
			remapper = remap(primaryMappings, jarPath, transformer, dataDir.resolve(version + "-client+srg.jar"));

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

		List<JsonObject> mods = ModManifestConverter.convertToFabric(manifest);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject primary = mods.get(0);
		JsonObject entrypoints = new JsonObject();

		entrypoints.add("patchwork", patchworkEntrypoints);
		primary.add("entrypoints", entrypoints);

		JsonArray jarsArray = new JsonArray();
		mods.forEach(m -> {
			if (m != primary) {
				String modid = m.getAsJsonPrimitive("id").getAsString();
				JsonObject file = new JsonObject();
				file.addProperty("file", "META-INF/jars/" + modid + ".jar");
				jarsArray.add(file);
				m.getAsJsonObject("custom").addProperty("modmenu:parent", primary.getAsJsonPrimitive("id").getAsString());
			}
		});

		primary.add("jars", jarsArray);
		String json = gson.toJson(primary);

		URI outputJar = new URI("jar:" + output.toUri().toString());
		FileSystem fs = FileSystems.newFileSystem(outputJar, Collections.emptyMap());
		Path fabricModJson = fs.getPath("/fabric.mod.json");

		try {
			Files.delete(fabricModJson);
		} catch (IOException ignored) {
			// ignored
		}

		Files.write(fabricModJson, json.getBytes(StandardCharsets.UTF_8));

		LOGGER.trace("fabric.mod.json: " + json);

		// Write patchwork logo
		this.writeLogo(primary, fs);

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

			// generate the jar
			Path subJarPath = Paths.get("temp/" + modid + ".jar");
			Map<String, String> env = new HashMap<>();
			env.put("create", "true");
			FileSystem subFs = FileSystems.newFileSystem(new URI("jar:" + subJarPath.toUri().toString()), env);

			// Write patchwork logo
			this.writeLogo(entry, subFs);

			// Write the fabric.mod.json
			Path modJsonPath = subFs.getPath("/fabric.mod.json");
			Files.write(modJsonPath, entry.toString().getBytes(StandardCharsets.UTF_8));

			subFs.close();

			Files.write(fs.getPath("/META-INF/jars/" + modid + ".jar"), Files.readAllBytes(subJarPath));

			Files.delete(subJarPath);
		}

		Path manifestPath = fs.getPath("/META-INF/mods.toml");
		Files.delete(manifestPath);
		Files.delete(fs.getPath("pack.mcmeta"));
		fs.close();

		// Late entrypoints
		// https://github.com/CottonMC/Cotton/blob/master/modules/cotton-datapack/src/main/java/io/github/cottonmc/cotton/datapack/mixins/MixinCottonInitializerServer.java
	}

	private void finish() {
		this.manifestRemapper.close();
		this.closed = true;
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

	private static TinyRemapper remap(IMappingProvider mappings, Path input, BiConsumer<String, byte[]> consumer, Path... classpath) {
		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(mappings).rebuildSourceFilenames(true).build();

		remapper.readClassPath(classpath);
		remapper.readInputs(input);
		remapper.apply(consumer);

		return remapper;
	}

	private void writeLogo(JsonObject json, FileSystem fs) throws IOException {
		if (json.getAsJsonPrimitive("icon").getAsString().equals("assets/patchwork-generated/icon.png")) {
			Files.createDirectories(fs.getPath("assets/patchwork-generated/"));
			Files.write(fs.getPath("assets/patchwork-generated/icon.png"), patchworkGreyscaleIcon);
		}
	}

	private void generateDevJar(Path relativeJarPath) {
		Path patchedJarPath = outputDir.resolve(relativeJarPath);
		String mod = patchedJarPath.getFileName().toString().split("\\.jar")[0];

		for (IMappingProvider mappingProvider : devMappings) {
			try {
				remap(
						mappingProvider, patchedJarPath,
						outputDir.resolve(mod + "-dev" + ".jar"),
						dataDir.resolve(version + "-client+intermediary.jar")
				);
				LOGGER.info("Dev jar generated %s", relativeJarPath);
			} catch (IOException ex) {
				LOGGER.thrown(LogLevel.ERROR, ex);
			}
		}
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

		Path inputDir = Files.createDirectories(currentPath.resolve("input"));
		Path outputDir = Files.createDirectories(currentPath.resolve("output"));
		Path tempDir = Files.createTempDirectory(currentPath, "temp");
		new Patchwork(inputDir, outputDir, currentPath.resolve("data/"), tempDir, bridged, Collections.emptyList()).patchAndFinish();
	}
}
