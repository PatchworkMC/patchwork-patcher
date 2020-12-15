package net.patchworkmc.patcher;

import java.io.File;
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
import java.util.Collection;
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
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import net.patchworkmc.manifest.accesstransformer.v2.ForgeAccessTransformer;
import net.patchworkmc.manifest.api.Remapper;
import net.patchworkmc.manifest.mod.ManifestParseException;
import net.patchworkmc.manifest.mod.ModManifest;
import net.patchworkmc.patcher.annotation.AnnotationStorage;
import net.patchworkmc.patcher.manifest.converter.accesstransformer.AccessTransformerConverter;
import net.patchworkmc.patcher.manifest.converter.mod.ModManifestConverter;
import net.patchworkmc.patcher.mapping.MemberInfo;
import net.patchworkmc.patcher.mapping.remapper.ManifestRemapperImpl;
import net.patchworkmc.patcher.mapping.remapper.PatchworkRemapper;
import net.patchworkmc.patcher.transformer.PatchworkTransformer;
import net.patchworkmc.patcher.util.MinecraftVersion;
import net.patchworkmc.patcher.util.ResourceDownloader;
import net.patchworkmc.patcher.util.VersionResolver;

public class Patchwork {
	// TODO use a "standard" log4j logger
	public static final Logger LOGGER = LogManager.getFormatterLogger("Patchwork");
	private byte[] patchworkGreyscaleIcon;

	private final MinecraftVersion minecraftVersion;
	private final Path inputDir, outputDir, tempDir;
	private final Path minecraftJarSrg, forgeUniversalJar;
	private final IMappingProvider primaryMappings;
	private final PatchworkRemapper patchworkRemapper;
	private final Remapper accessTransformerRemapper;
	private final MemberInfo memberInfo;
	private boolean closed = false;

	/**
	 * @param inputDir
	 * @param outputDir
	 * @param tempDir
	 * @param primaryMappings mappings in the format of {@code source -> target}
	 * @param targetFirstMappings mappings in the format of {@code target -> any}
	 */
	public Patchwork(MinecraftVersion minecraftVersion, Path inputDir, Path outputDir, Path minecraftJar, Path forgeUniversalJar, Path tempDir, IMappingProvider primaryMappings, IMappingProvider targetFirstMappings) {
		this.minecraftVersion = minecraftVersion;
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.tempDir = tempDir;
		this.minecraftJarSrg = minecraftJar;
		this.forgeUniversalJar = forgeUniversalJar;
		this.primaryMappings = primaryMappings;
		this.memberInfo = new MemberInfo(targetFirstMappings);

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

		// If any exceptions are encountered during remapping they are caught and the ForgeModJar's "processed" boolean will not be true.
		LOGGER.warn("Patching %s mods", mods.size());
		remapJars(mods, this.minecraftJarSrg, this.forgeUniversalJar);

		for (ForgeModJar mod : mods) {
			try {
				// TODO: technically this could be done in parallel, but in my (Glitch) testing rewriting metadata of over 150 mods only
				//  took a little over a second
				rewriteMetadata(mod);
				count++;
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

		return new ForgeModJar(jarPath, outputDir.resolve(jarPath.getFileName()), manifest, at);
	}

	private void rewriteMetadata(ForgeModJar forgeModJar) throws IOException, URISyntaxException {
		Path output = forgeModJar.getOutputPath();

		if (!forgeModJar.isProcessed()) {
			LOGGER.warn("Skipping %s because it has not been successfully remapped!", forgeModJar.getOutputPath().getFileName());
			return;
		}

		ModManifest manifest = forgeModJar.getManifest();
		AnnotationStorage annotationStorage = forgeModJar.getAnnotationStorage();
		String mod = output.getFileName().toString().split("\\.jar")[0];
		LOGGER.info("Rewriting mod metadata for %s", mod);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		List<JsonObject> mods = ModManifestConverter.convertToFabric(manifest);

		JsonObject primary = mods.get(0);
		String primaryModId = primary.getAsJsonPrimitive("id").getAsString();
		primary.add("entrypoints", forgeModJar.getEntrypoints());

		JsonArray jarsArray = new JsonArray();

		for (JsonObject m : mods) {
			if (m != primary) {
				String modid = m.getAsJsonPrimitive("id").getAsString();
				JsonObject file = new JsonObject();
				file.addProperty("file", "META-INF/jars/" + modid + ".jar");
				jarsArray.add(file);
				JsonObject custom = m.getAsJsonObject("custom");
				// TODO: move to ModManifestConverter
				custom.addProperty("modmenu:parent", primaryModId);
			}

			if (!annotationStorage.isEmpty()) {
				m.getAsJsonObject("custom").getAsJsonObject("patchwork:patcherMeta")
						.addProperty("annotations", AnnotationStorage.relativePath);
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
	}

	private void finish() {
		this.closed = true;
	}

	public static void remap(IMappingProvider mappings, Path input, Path output, Path... classpath)
			throws IOException {
		TinyRemapper remapper = null;

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build()) {
			remapper = remap(mappings, input, outputConsumer, classpath);
			outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);
		} finally {
			if (remapper != null) {
				remapper.finish();
			}
		}
	}

	private static TinyRemapper remap(IMappingProvider mappings, Path input, BiConsumer<String, byte[]> consumer, Path... classpath) {
		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(mappings).rebuildSourceFilenames(true).build();

		remapper.readClassPath(classpath);
		remapper.readInputs(input);
		remapper.apply(consumer);

		return remapper;
	}

	private void remapJars(Collection<ForgeModJar> jars, Path... classpath) {
		final ArrayList<PatchworkTransformer> outputConsumers = new ArrayList<>();
		TinyRemapper remapper = TinyRemapper.newRemapper().withMappings(this.primaryMappings).rebuildSourceFilenames(true).build();

		try {
			remapper.readClassPathAsync(classpath);

			final Map<ForgeModJar, InputTag> tagMap = new HashMap<>();

			for (ForgeModJar jar : jars) {
				InputTag tag = remapper.createInputTag();
				remapper.readInputsAsync(tag, jar.getInputPath());
				tagMap.put(jar, tag);
			}

			for (ForgeModJar forgeModJar : jars) {
				try {
					Files.deleteIfExists(forgeModJar.getOutputPath());
					Path jar = forgeModJar.getInputPath();
					PatchworkTransformer transformer = new PatchworkTransformer(this.minecraftVersion, new OutputConsumerPath.Builder(forgeModJar.getOutputPath()).build(), forgeModJar);
					outputConsumers.add(transformer);
					remapper.apply(transformer, tagMap.get(forgeModJar));
					transformer.finish();
					transformer.getOutputConsumer().addNonClassFiles(jar, NonClassCopyMode.FIX_META_INF, remapper);
					transformer.closeOutputConsumer();
					forgeModJar.processed = true;
				} catch (Exception ex) {
					LOGGER.error("Skipping remapping mod %s due to errors:", forgeModJar.getInputPath().getFileName());
					LOGGER.throwing(Level.ERROR, ex);
				}
			}
		} finally {
			// hopefully prevent leaks
			remapper.finish();
			outputConsumers.forEach(PatchworkTransformer::closeOutputConsumer);
		}
	}

	private void writeLogo(JsonObject json, FileSystem fs) throws IOException {
		if (json.getAsJsonPrimitive("icon").getAsString().equals("assets/patchwork-generated/icon.png")) {
			Files.createDirectories(fs.getPath("assets/patchwork-generated/"));
			Files.write(fs.getPath("assets/patchwork-generated/icon.png"), patchworkGreyscaleIcon);
		}
	}

	public static Patchwork create(Path inputDir, Path outputDir, Path dataDir, MinecraftVersion minecraftVersion) throws IOException, URISyntaxException {
		try {
			return createInner(inputDir, outputDir, dataDir, minecraftVersion);
		} catch (IOException | URISyntaxException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("Couldn't setup Patchwork!", e);

			throw new RuntimeException("Couldn't setup Patchwork!", e);
		}
	}

	private static Patchwork createInner(Path inputDir, Path outputDir, Path dataDir, MinecraftVersion minecraftVersion) throws IOException, URISyntaxException {
		Files.createDirectories(inputDir);
		Files.createDirectories(outputDir);
		Files.createDirectories(dataDir);
		Path tempDir = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "patchwork-patcher-");

		ResourceDownloader downloader = new ResourceDownloader(minecraftVersion);

		String forgeVersion = VersionResolver.getForgeVersion(minecraftVersion);
		Path forgeUniversal = dataDir.resolve("forge-universal-" + forgeVersion + ".jar");

		if (!Files.exists(forgeUniversal)) {
			Files.walk(dataDir).filter((path -> {
				return path.getFileName().toString().startsWith("forge-universal-" + minecraftVersion.getVersion());
			})).forEach((path -> {
				try {
					Files.delete(path);
				} catch (IOException ex) {
					LOGGER.error("Unable to delete old Forge version at " + path);
					LOGGER.throwing(ex);
				}
			}));

			downloader.downloadForgeUniversal(forgeUniversal, forgeVersion);
		}

		Path minecraftJar = dataDir.resolve("minecraft-merged-srg-" + minecraftVersion.getVersion() + ".jar");

		boolean mappingsCached = false;

		if (!Files.exists(minecraftJar)) {
			LOGGER.warn("Merged minecraft jar not found, generating one!");
			downloader.createAndRemapMinecraftJar(minecraftJar);
			mappingsCached = true;
			LOGGER.warn("Done");
		}

		Path mappings = Files.createDirectories(dataDir.resolve("mappings")).resolve("voldemap-bridged-" + minecraftVersion.getVersion() + ".tiny");

		IMappingProvider bridgedMappings;

		if (!Files.exists(mappings)) {
			if (!mappingsCached) {
				LOGGER.warn("Mappings not cached, downloading!");
			}

			bridgedMappings = downloader.setupAndLoadMappings(mappings, minecraftJar);

			if (!mappingsCached) {
				LOGGER.warn("Done");
			}
		} else if (mappingsCached) {
			bridgedMappings = downloader.setupAndLoadMappings(null, minecraftJar);
		} else {
			bridgedMappings = TinyUtils.createTinyMappingProvider(mappings, "srg", "intermediary");
		}

		IMappingProvider bridgedInverted = TinyUtils.createTinyMappingProvider(mappings, "intermediary", "srg");

		return new Patchwork(minecraftVersion, inputDir, outputDir, minecraftJar, forgeUniversal, tempDir, bridgedMappings, bridgedInverted);
	}
}
