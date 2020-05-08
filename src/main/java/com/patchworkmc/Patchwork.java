package com.patchworkmc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import com.patchworkmc.jar.ForgeModJar;
import net.patchworkmc.manifest.accesstransformer.AccessTransformerList;
import com.patchworkmc.manifest.converter.accesstransformer.FieldDescriptorProvider;
import com.patchworkmc.manifest.converter.accesstransformer.GloomDefinitionParser;
import com.patchworkmc.manifest.converter.mod.ModManifestConverter;
import net.patchworkmc.manifest.mod.ManifestParseException;
import net.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.mapping.BridgedMappings;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.TinyWriter;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import com.patchworkmc.mapping.remapper.AccessTransformerRemapper;
import com.patchworkmc.mapping.remapper.NaiveRemapper;
import com.patchworkmc.transformer.PatchworkTransformer;

public class Patchwork {
	public static final Logger LOGGER = LogManager.getFormatterLogger("Patchwork");
	private static String version = "1.14.4";

	private byte[] patchworkGreyscaleIcon;

	private Path inputDir, outputDir, dataDir, tempDir;
	private Path clientJarSrg;
	private IMappingProvider primaryMappings;
	private IMappingProvider invertedMappings;
	private List<IMappingProvider> devMappings;
	private FieldDescriptorProvider fieldDescriptorProvider;
	private NaiveRemapper naiveRemapper;
	private AccessTransformerRemapper accessTransformerRemapper;
	private boolean closed = false;

	public Patchwork(Path inputDir, Path outputDir, Path dataDir, Path tempDir, Path bridgedMappings, List<IMappingProvider> devMappings) {
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.dataDir = dataDir;
		this.tempDir = tempDir;
		this.clientJarSrg = dataDir.resolve(version + "-client+srg.jar");
		this.primaryMappings = TinyUtils.createTinyMappingProvider(bridgedMappings, "srg", "intermediary");
		this.invertedMappings = TinyUtils.createTinyMappingProvider(bridgedMappings, "intermediary", "srg");

		this.devMappings = devMappings;
		// Java doesn't delete temporary folders by default.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				FileUtils.deleteDirectory(this.tempDir.toFile());
			} catch (FileNotFoundException ignored) {
				// NO-OP; the file is already deleted
			} catch (IOException ex) {
				LOGGER.throwing(Level.WARN, ex);
			}
		}));

		try (InputStream inputStream = Patchwork.class.getResourceAsStream("/patchwork-icon-greyscale.png")) {
			this.patchworkGreyscaleIcon = new byte[inputStream.available()];
			inputStream.read(this.patchworkGreyscaleIcon);
		} catch (IOException ex) {
			LOGGER.throwing(Level.FATAL, ex);
		}

		this.fieldDescriptorProvider = new FieldDescriptorProvider(this.primaryMappings);
		this.naiveRemapper = new NaiveRemapper(this.primaryMappings);
		this.accessTransformerRemapper = new AccessTransformerRemapper(this.primaryMappings);
	}

	public int patchAndFinish() throws IOException {
		if (this.closed) {
			throw new IllegalStateException("Cannot begin patching: Already patched all mods!");
		}

		List<ForgeModJar> mods;
		int count = 0;

		try (Stream<Path> inputFilesStream = Files.walk(inputDir).filter(file -> file.toString().endsWith(".jar"))) {
			mods = parseAllManifests(inputFilesStream);
		}

		for (ForgeModJar mod : mods) {
			try {
				transformMod(mod);
				count++;

				generateDevJarsForOneModJar(mod);
			} catch (Exception ex) {
				LOGGER.throwing(Level.ERROR, ex);
			}
		}

		finish();
		return count;
	}

	private List<ForgeModJar> parseAllManifests(Stream<Path> modJars) {
		ArrayList<ForgeModJar> mods = new ArrayList<>();

		modJars.forEach((jarPath -> {
			try {
				mods.add(parseModManifest(jarPath));
			} catch (IOException | URISyntaxException | ManifestParseException ex) {
				LOGGER.throwing(Level.ERROR, ex);
			}
		}));

		for (ForgeModJar mod : mods) {
			mod.addDependencyJars(mods);
		}

		return mods;
	}

	private ForgeModJar parseModManifest(Path jarPath) throws IOException, URISyntaxException, ManifestParseException {
		String mod = jarPath.getFileName().toString().split("\\.jar")[0];
		// Load metadata
		LOGGER.trace("Loading and parsing metadata for %s", mod);
		URI inputJar = new URI("jar:" + jarPath.toUri());

		FileConfig toml;
		AccessTransformerList accessTransformers = null;

		try (FileSystem fs = FileSystems.newFileSystem(inputJar, Collections.emptyMap())) {
			Path manifestPath = fs.getPath("/META-INF/mods.toml");
			toml = FileConfig.of(manifestPath);
			toml.load();

			try {
				accessTransformers = AccessTransformerList.parse(fs.getPath("/META-INF/accesstransformer.cfg"));
			} catch (Exception e) {
				LOGGER.throwing(Level.ERROR, new RuntimeException("Unable to parse access transformer list", e));
			}

			if (accessTransformers == null) {
				accessTransformers = new AccessTransformerList(new ArrayList<>());
			}
		}

		Map<String, Object> map = toml.valueMap();
		LOGGER.trace("\nRaw mod toml:");
		map.forEach((s, o) -> LOGGER.trace("  " + s + ": " + o));

		ModManifest manifest = ModManifest.parse(map);

		if (!manifest.getModLoader().equals("javafml")) {
			LOGGER.error("Unsupported modloader %s", manifest.getModLoader());
		}

		LOGGER.trace("Remapping access transformers");

		accessTransformers.remap(accessTransformerRemapper);

		return new ForgeModJar(jarPath, manifest, GloomDefinitionParser.parse(accessTransformers, fieldDescriptorProvider));
	}

	private void transformMod(ForgeModJar forgeModJar) throws IOException, URISyntaxException {
		Path jarPath = forgeModJar.getJarPath();
		ModManifest manifest = forgeModJar.getManifest();
		String mod = jarPath.getFileName().toString().split("\\.jar")[0];

		LOGGER.info("Remapping and patching %s (TinyRemapper, srg -> intermediary)", mod);
		Path output = outputDir.resolve(mod + ".jar");
		// Delete old patched jar
		Files.deleteIfExists(output);
		TinyRemapper remapper = null;

		OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build();
		PatchworkTransformer transformer = new PatchworkTransformer(outputConsumer, naiveRemapper);
		JsonArray patchworkEntrypoints = new JsonArray();

		try {
			remapper = remap(primaryMappings, jarPath, transformer, clientJarSrg);

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
		this.accessTransformerRemapper.close();
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

	private void generateDevJarsForOneModJar(ForgeModJar mod) {
		Path relativeJarPath = inputDir.relativize(mod.getJarPath());
		Path patchedJarPath = outputDir.resolve(relativeJarPath);
		String modName = patchedJarPath.getFileName().toString().split("\\.jar")[0];

		for (int i = 0; i < devMappings.size(); i++) {
			IMappingProvider mappingProvider = devMappings.get(i);

			try {
				remap(
						mappingProvider, patchedJarPath,
						outputDir.resolve(modName + "-dev-" + i + "-.jar"),
						dataDir.resolve(version + "-client+intermediary.jar")
				);
				LOGGER.info("Dev jar generated %s", relativeJarPath);
			} catch (IOException ex) {
				LOGGER.throwing(Level.ERROR, ex);
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


		if (!voldemapBridged.exists()) {

			LOGGER.trace("Generating bridged (srg -> intermediary) tiny mappings");

			TinyWriter tinyWriter = new TinyWriter("srg", "intermediary");
			IMappingProvider bridged = new BridgedMappings(mappings, intermediary);
			bridged.load(tinyWriter);

			Files.write(voldemapBridged.toPath(), tinyWriter.toString().getBytes(StandardCharsets.UTF_8));
		} else {
			LOGGER.trace("Using cached bridged (srg -> intermediary) tiny mappings");
		}

		Path inputDir = Files.createDirectories(currentPath.resolve("input"));
		Path outputDir = Files.createDirectories(currentPath.resolve("output"));
		Path tempDir = Files.createTempDirectory(currentPath, "temp");
		new Patchwork(inputDir, outputDir, currentPath.resolve("data/"), tempDir, voldemapBridged.toPath(), Collections.emptyList()).patchAndFinish();
	}
}
