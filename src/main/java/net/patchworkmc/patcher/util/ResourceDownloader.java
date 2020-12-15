package net.patchworkmc.patcher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.stitch.merge.JarMerger;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;

import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.mapping.BridgedMappings;
import net.patchworkmc.patcher.mapping.RawMapping;
import net.patchworkmc.patcher.mapping.TinyWriter;
import net.patchworkmc.patcher.mapping.Tsrg;
import net.patchworkmc.patcher.mapping.TsrgClass;
import net.patchworkmc.patcher.mapping.TsrgMappings;

public final class ResourceDownloader {
	protected static final String FORGE_MAVEN = "https://files.minecraftforge.net/maven";
	private static final String FABRIC_MAVEN = "https://maven.fabricmc.net";
	private static final Logger LOGGER = LogManager.getLogger(ResourceDownloader.class);

	private final Path tempDir;
	private final MinecraftVersion minecraftVersion;
	private IMappingProvider srg;
	private IMappingProvider bridged;

	public ResourceDownloader(MinecraftVersion minecraftVersion) {
		try {
			tempDir = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(),
				"patchwork-patcher-ResourceDownloader-");
		} catch (IOException ex) {
			throw new UncheckedIOException("Unable to create temp folder!", ex);
		}

		this.minecraftVersion = minecraftVersion;
	}

	public void createAndRemapMinecraftJar(Path minecraftJar) throws IOException, URISyntaxException {
		LOGGER.info("Downloading Minecraft jars");
		Path client = tempDir.resolve("minecraft-client.jar");
		Path server = tempDir.resolve("minecraft-server.jar");
		downloadMinecraftJars(client, server);
		Path obfJar = tempDir.resolve("minecraft-merged.jar");

		try (JarMerger jarMerger = new JarMerger(client.toFile(), server.toFile(), obfJar.toFile())) {
			jarMerger.enableSyntheticParamsOffset();
			LOGGER.info("Merging and remapping Minecraft jars");
			LOGGER.trace(": merging jars");
			jarMerger.merge();
		}

		if (srg == null) {
			// Will cache mappings in memory so when you come along to write them later it skips all that work
			setupAndLoadMappings(null, obfJar);
		}

		LOGGER.trace(": remapping Minecraft jar");
		Patchwork.remap(this.srg, obfJar, minecraftJar);
	}

	public void downloadForgeUniversal(Path forgeUniversalJar, String forgeVersion) throws IOException {
		FileUtils.copyURLToFile(new URL(FORGE_MAVEN + "/net/minecraftforge/forge/" + forgeVersion
				+ "/forge-" + forgeVersion + "-universal.jar"), forgeUniversalJar.toFile());
	}

	public IMappingProvider setupAndLoadMappings(Path voldemapBridged, Path mergedMinecraftJar) throws IOException, URISyntaxException {
		// TODO: use lorenz instead of coderbot's home-grown solution
		// waiting on https://github.com/CadixDev/Lorenz/pull/38 for this
		if (this.srg == null) {
			LOGGER.trace(": downloading mappings");

			Path srg = tempDir.resolve("srg.tsrg");
			downloadSrg(srg);
			Path intermediary = tempDir.resolve("intermediary.tiny");
			downloadIntermediary(intermediary);
			IMappingProvider intermediaryProvider = TinyUtils.createTinyMappingProvider(intermediary, "official", "intermediary");

			List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(new FileInputStream(srg.toFile()));

			purgeNonexistentClassMappings(classes, mergedMinecraftJar);

			TsrgMappings tsrgMappings = new TsrgMappings(classes, intermediaryProvider);
			this.srg = tsrgMappings;
			this.bridged = new BridgedMappings(tsrgMappings, intermediaryProvider);
		}

		if (voldemapBridged != null) {
			TinyWriter tinyWriter = new TinyWriter("srg", "intermediary");
			this.bridged.load(tinyWriter);
			Files.write(voldemapBridged, tinyWriter.toString().getBytes(StandardCharsets.UTF_8));
		}

		return bridged;
	}

	/**
	 * Purges SRG mappings for classes that don't actually exist in the merged Minecraft jar
	 *
	 * <p>Newer versions of MCPConfig include mappings for classes (currently, "afg" and "dew" in 1.16.4) that do not
	 * exist in either the Minecraft client or server jars for 1.16.4. It's unclear why exactly these mappings are
	 * present, but interestingly enough, they are also present in the 1.16.4 Mojang mappings (client.txt / server.txt).
	 * It seems likely that whatever automated tools the MCPConfig/Forge team use to update their mappings get confused
	 * by this and retain the orphaned mappings.</p>
	 *
	 * <p>Unfortunately, some of our code doesn't particularly like these missing mappings. The code that combines the
	 * SRG mappings (official->srg) and intermediary mappings (official->intermediary) into bridged mappings
	 * (srg->intermediary) in particular has issues when it tries to locate the orphaned mappings within intermediary,
	 * since intermediary doesn't contain the orphaned mappings. Therefore, we remove the orphaned mappings by checking
	 * whether each given class actually exists in the Minecraft jar, so that they don't cause any problems.</p>
	 */
	private void purgeNonexistentClassMappings(List<TsrgClass<RawMapping>> classes, Path mergedMinecraftJar) throws URISyntaxException, IOException {
		// I'm using an iterator here since it allows us to remove entries easily as we iterate over the List
		Iterator<TsrgClass<RawMapping>> classIterator = classes.iterator();

		URI jarUri = new URI("jar:" + mergedMinecraftJar.toUri());

		boolean needsClarificationMessage = false;

		try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
			while (classIterator.hasNext()) {
				TsrgClass<RawMapping> clazz = classIterator.next();

				String officialName = clazz.getOfficial();
				Path path = fs.getPath("/" + officialName + ".class");

				if (!Files.exists(path)) {
					LOGGER.warn("The class " + clazz.getOfficial() + " (MCP Name: " + clazz.getMapped() + ") has an SRG mapping but is not actually present in the merged Minecraft jar!");
					needsClarificationMessage = true;

					classIterator.remove();
				}
			}
		}

		if (needsClarificationMessage) {
			// I print the above warnings so that they can be cross-checked if necessary for debugging, however I don't
			// want someone just casually reading the log to be concerned since they're nominal otherwise.
			//
			// Therefore, print a single message stating that the warnings are nominal.
			LOGGER.warn("Please note that the above warnings are expected on newer versions of Minecraft (such as 1.16.4)");
		}
	}

	private void downloadMinecraftJars(Path client, Path server) throws IOException {
		Path versionManifest = tempDir.resolve("mc-version-manifest.json");
		FileUtils.copyURLToFile(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"), versionManifest.toFile());

		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		JsonArray versions = gson.fromJson(new String(Files.readAllBytes(versionManifest), StandardCharsets.UTF_8), JsonObject.class)
				.get("versions").getAsJsonArray();

		for (JsonElement jsonElement : versions) {
			if (jsonElement.isJsonObject()) {
				JsonObject object = jsonElement.getAsJsonObject();
				String id = object.get("id").getAsJsonPrimitive().getAsString();

				if (id.equals(minecraftVersion.getVersion())) {
					String versionUrl = object.get("url").getAsJsonPrimitive().getAsString();
					JsonObject downloads = gson.fromJson(new InputStreamReader(new URL(versionUrl).openStream()), JsonObject.class)
							.getAsJsonObject("downloads");
					String clientJarUrl = downloads.getAsJsonObject("client").get("url").getAsJsonPrimitive().getAsString();
					String serverJarUrl = downloads.getAsJsonObject("server").get("url").getAsJsonPrimitive().getAsString();
					LOGGER.trace(": downloading client jar");
					FileUtils.copyURLToFile(new URL(clientJarUrl), client.toFile());
					LOGGER.trace(": downloading server jar");
					FileUtils.copyURLToFile(new URL(serverJarUrl), server.toFile());
					break;
				}
			}
		}
	}

	private void downloadSrg(Path mcp) throws IOException, URISyntaxException {
		LOGGER.trace("      : downloading SRG");
		FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/"
				+ "release/" + minecraftVersion.getVersion() + "/joined.tsrg"), mcp.toFile());
	}

	private void downloadIntermediary(Path intermediary) throws IOException, URISyntaxException {
		LOGGER.trace("      : downloading Intermediary");
		Path intJar = tempDir.resolve("intermediary.jar");
		FileUtils.copyURLToFile(new URL(FABRIC_MAVEN + "/net/fabricmc/intermediary/" + minecraftVersion.getVersion() + "/intermediary-"
				+ minecraftVersion.getVersion() + ".jar"), intJar.toFile());

		URI inputJar = new URI("jar:" + intJar.toUri());

		try (FileSystem fs = FileSystems.newFileSystem(inputJar, Collections.emptyMap())) {
			Files.copy(fs.getPath("/mappings/mappings.tiny"), intermediary);
		}
	}

	private Path createTempDirectory(String name) throws IOException {
		return Files.createDirectory(tempDir.resolve(name));
	}
}
