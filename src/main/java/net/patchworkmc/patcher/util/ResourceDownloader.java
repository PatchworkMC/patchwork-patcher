package net.patchworkmc.patcher.util;

import java.io.File;
import java.io.FileReader;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.srg.tsrg.TSrgReader;
import org.cadixdev.lorenz.merge.MappingSetMerger;
import org.cadixdev.lorenz.merge.MergeConfig;

import net.fabricmc.stitch.merge.JarMerger;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;

import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.mapping.PatchworkMappings;
import net.patchworkmc.patcher.mapping.PatchworkMergerHandler;
import net.patchworkmc.patcher.mapping.TinyWriter;

public final class ResourceDownloader {
	protected static final String FORGE_MAVEN = "https://files.minecraftforge.net/maven";
	private static final String FABRIC_MAVEN = "https://maven.fabricmc.net";
	private static final Logger LOGGER = LogManager.getLogger(ResourceDownloader.class);

	private final Path tempDir;
	private final MinecraftVersion minecraftVersion;
	private PatchworkMappings srg;
	private PatchworkMappings bridged;

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
			setupAndLoadMappings(null);
		}

		LOGGER.trace(": remapping Minecraft jar to srg");
		Patchwork.remap(this.srg, obfJar, minecraftJar);
	}

	public void downloadForgeUniversal(Path forgeUniversalJar, String forgeVersion) throws IOException {
		FileUtils.copyURLToFile(new URL(FORGE_MAVEN + "/net/minecraftforge/forge/" + forgeVersion
			+ "/forge-" + forgeVersion + "-universal.jar"), forgeUniversalJar.toFile());
	}

	public PatchworkMappings setupAndLoadMappings(Path voldemapBridged) throws IOException, URISyntaxException {
		if (this.srg == null) {
			LOGGER.trace(": downloading mappings");

			Path srg = tempDir.resolve("srg.tsrg");
			downloadSrg(srg);
			Path intermediary = tempDir.resolve("intermediary.tiny");
			downloadIntermediary(intermediary);
			IMappingProvider intermediaryProvider = TinyUtils.createTinyMappingProvider(intermediary, "official", "intermediary");

			// TSrg fields don't have signatures (which we need for things like access wideners).
			// To get around this, we create an int->srg mappingset, which Lorenz will correctly repair mappings for, and then reverse the final set
			MappingSet intToObf = new PatchworkMappings(intermediaryProvider).getLorenzMappings().reverse();

			MappingSet obfToSrg;
			try (TSrgReader reader = new TSrgReader(new FileReader(srg.toFile()))) {
				obfToSrg = reader.read();
			}
			//this.srg = srg
			MappingSetMerger merger = MappingSetMerger.create(intToObf, obfToSrg, MergeConfig.builder().withMergeHandler(new PatchworkMergerHandler()).build());
			// <some hack to repair the field signatures for srg>
			this.bridged = new PatchworkMappings(merger.merge().reverse());
			throw new Error("hack not implemented yet");
		}

		if (voldemapBridged != null) {
			TinyWriter tinyWriter = new TinyWriter("srg", "intermediary");
			this.bridged.load(tinyWriter);
			Files.write(voldemapBridged, tinyWriter.toString().getBytes(StandardCharsets.UTF_8));
		}

		return bridged;
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
		Path mcpConfig = tempDir.resolve("mcp-config.zip");
		String mcpVersion = VersionResolver.getMcpVersion(this.minecraftVersion);
		FileUtils.copyURLToFile(new URL(FORGE_MAVEN + "/de/oceanlabs/mcp/mcp_config/" + mcpVersion
				+ "/mcp_config-" + mcpVersion + ".zip"), mcpConfig.toFile());
		// the jar loader opens zips just fine
		URI inputJar = new URI("jar:" + mcpConfig.toUri());

		try (FileSystem fs = FileSystems.newFileSystem(inputJar, Collections.emptyMap())) {
			Files.copy(fs.getPath("/config/joined.tsrg"), mcp);
		}
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
