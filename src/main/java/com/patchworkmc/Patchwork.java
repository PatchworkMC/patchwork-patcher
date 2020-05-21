package com.patchworkmc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import net.patchworkmc.manifest.accesstransformer.v2.ForgeAccessTransformer;
import net.patchworkmc.manifest.api.Remapper;
import net.patchworkmc.manifest.mod.ManifestParseException;
import net.patchworkmc.manifest.mod.ModManifest;

import com.patchworkmc.annotation.AnnotationStorage;
import com.patchworkmc.jar.ForgeModJar;
import com.patchworkmc.manifest.converter.accesstransformer.AccessTransformerConverter;
import com.patchworkmc.manifest.converter.mod.ModManifestConverter;
import com.patchworkmc.mapping.BridgedMappings;
import com.patchworkmc.mapping.MemberInfo;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.TinyWriter;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import com.patchworkmc.mapping.remapper.ManifestRemapperImpl;
import com.patchworkmc.mapping.remapper.PatchworkRemapper;
import com.patchworkmc.transformer.PatchworkTransformer;

public class Patchwork {
	public static final Logger LOGGER = LogManager.getFormatterLogger("Patchwork");
	private static String version = "1.14.4";

	private byte[] patchworkGreyscaleIcon;

	private Path inputDir, outputDir, dataDir, tempDir;
	private Path clientJarSrg;
	private IMappingProvider primaryMappings;
	private List<IMappingProvider> devMappings;
	private PatchworkRemapper patchworkRemapper;
	private Remapper accessTransformerRemapper;
	private final MemberInfo memberInfo;
	private boolean closed = false;

	/**
	 * @param inputDir
	 * @param outputDir
	 * @param dataDir
	 * @param tempDir
	 * @param primaryMappings mappings in the format of {@code source -> target}
	 * @param targetFirstMappings mappings in the format of {@code target -> any}
	 * @param devMappings any additional mappings needed after the main remapping stage (Doesn't work for ATs or reflection)
	 */
	public Patchwork(Path inputDir, Path outputDir, Path dataDir, Path tempDir, IMappingProvider primaryMappings, IMappingProvider targetFirstMappings, List<IMappingProvider> devMappings) {
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.dataDir = dataDir;
		this.tempDir = tempDir;
		this.clientJarSrg = dataDir.resolve(version + "-client+srg.jar");
		this.primaryMappings = primaryMappings;
		this.memberInfo = new MemberInfo(targetFirstMappings);

		this.devMappings = devMappings;

		try (InputStream inputStream = Patchwork.class.getResourceAsStream("/patchwork-icon-greyscale.png")) {
			this.patchworkGreyscaleIcon = new byte[inputStream.available()];
			inputStream.read(this.patchworkGreyscaleIcon);
		} catch (IOException ex) {
			LOGGER.throwing(Level.FATAL, ex);
		}

		this.patchworkRemapper = new PatchworkRemapper(this.primaryMappings);
		this.accessTransformerRemapper = new ManifestRemapperImpl(this.primaryMappings, this.patchworkRemapper);
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
			} catch (Exception ex) {
				LOGGER.throwing(Level.ERROR, ex);
			}
		}));

		return mods;
	}

	private ForgeModJar parseModManifest(Path jarPath) throws IOException, URISyntaxException, ManifestParseException {
		String mod = jarPath.getFileName().toString().split("\\.jar")[0];
		// Load metadata
		LOGGER.trace("Loading and parsing metadata for %s", mod);
		URI inputJar = new URI("jar:" + jarPath.toUri());

		FileConfig toml;
		ForgeAccessTransformer at = null;

		try (FileSystem fs = FileSystems.newFileSystem(inputJar, Collections.emptyMap())) {
			Path manifestPath = fs.getPath("/META-INF/mods.toml");
			toml = FileConfig.of(manifestPath);
			toml.load();

			Path atPath = fs.getPath("/META-INF/accesstransformer.cfg");

			if (Files.exists(atPath)) {
				at = ForgeAccessTransformer.parse(atPath);
			}
		}

		Map<String, Object> map = toml.valueMap();

		ModManifest manifest = ModManifest.parse(map);

		if (!manifest.getModLoader().equals("javafml")) {
			LOGGER.error("Unsupported modloader %s", manifest.getModLoader());
		}

		if (at != null) {
			at.remap(accessTransformerRemapper, ex -> LOGGER.throwing(Level.WARN, ex));
		}

		return new ForgeModJar(jarPath, manifest, at);
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
		AnnotationStorage annotationStorage = new AnnotationStorage();
		PatchworkTransformer transformer = new PatchworkTransformer(outputConsumer, patchworkRemapper, annotationStorage);
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

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		List<JsonObject> mods = ModManifestConverter.convertToFabric(manifest);

		JsonObject primary = mods.get(0);
		JsonObject entrypoints = new JsonObject();
		String primaryModId = primary.getAsJsonPrimitive("id").getAsString();

		entrypoints.add("patchwork", patchworkEntrypoints);
		primary.add("entrypoints", entrypoints);

		JsonArray jarsArray = new JsonArray();

		for (JsonObject m : mods) {
			if (m != primary) {
				String modid = m.getAsJsonPrimitive("id").getAsString();
				JsonObject file = new JsonObject();
				file.addProperty("file", "META-INF/jars/" + modid + ".jar");
				jarsArray.add(file);
				JsonObject custom = m.getAsJsonObject("custom");
				custom.addProperty("modmenu:parent", primaryModId);
				custom.addProperty("patchwork:parent", primaryModId);
			}

			if (!annotationStorage.isEmpty()) {
				m.getAsJsonObject("custom").addProperty(
						"patchwork:annotations", AnnotationStorage.relativePath
				);
			}
		}

		primary.add("jars", jarsArray);

		String modid = primary.getAsJsonPrimitive("id").getAsString();
		ForgeAccessTransformer at = forgeModJar.getAccessTransformer();
		String accessWidenerName = modid + ".accessWidener";

		if (at != null) {
			primary.addProperty("accessWidener", accessWidenerName);
		}

		String json = gson.toJson(primary);

		URI outputJar = new URI("jar:" + output.toUri().toString());
		FileSystem fs = FileSystems.newFileSystem(outputJar, Collections.emptyMap());
		Path fabricModJson = fs.getPath("/fabric.mod.json");

		try {
			Files.delete(fabricModJson);

			if (at != null) {
				Files.delete(fs.getPath("/META-INF/accesstransformer.cfg"));
			}
		} catch (IOException ignored) {
			// ignored
		}

		Files.write(fabricModJson, json.getBytes(StandardCharsets.UTF_8));

		if (at != null) {
			Files.write(fs.getPath("/" + accessWidenerName), AccessTransformerConverter.convertToWidener(at, memberInfo));
		}

		// Write annotation data
		if (!annotationStorage.isEmpty()) {
			Path annotationJsonPath = fs.getPath(AnnotationStorage.relativePath);
			Files.write(annotationJsonPath, annotationStorage.toJson(gson).getBytes(StandardCharsets.UTF_8));
		}

		// Write patchwork logo
		this.writeLogo(primary, fs);

		try {
			Files.createDirectory(fs.getPath("/META-INF/jars/"));
		} catch (IOException ignored) {
			// ignored
		}

		for (JsonObject entry : mods) {
			if (entry == primary) {
				// Don't write the primary jar as a jar-in-jar!
				continue;
			}

			// generate the jar
			Path subJarPath = tempDir.resolve(modid + ".jar");
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
						outputDir.resolve(modName + "-dev-" + i + ".jar"),
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
		IMappingProvider bridged;
		IMappingProvider bridgedInverted;

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

		bridgedInverted = TinyUtils.createTinyMappingProvider(voldemapBridged.toPath(), "intermediary", "srg");

		Path inputDir = Files.createDirectories(currentPath.resolve("input"));
		Path outputDir = Files.createDirectories(currentPath.resolve("output"));
		Path tempDir = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "patchwork-patcher-cli");
		new Patchwork(inputDir, outputDir, currentPath.resolve("data/"), tempDir, bridged, bridgedInverted, Collections.emptyList()).patchAndFinish();
	}
}
