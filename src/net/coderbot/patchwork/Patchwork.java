package net.coderbot.patchwork;

import com.electronwill.toml.Toml;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.coderbot.patchwork.manifest.converter.ModManifestConverter;
import net.coderbot.patchwork.manifest.forge.ModManifest;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Patchwork {
	public static void main(String[] args) throws Exception {
		ZipFile file = new ZipFile(new File("data/BiomesOPlenty-1.14.4-9.0.0.253-universal.jar"));

		ZipEntry entry = file.getEntry("META-INF/mods.toml");

		if(entry == null) {
			System.err.println("Mod zip is not a Forge 1.13+ mod, it is missing a META-INF/mods.toml file");

			return;
		}

		InputStream manifestStream  = file.getInputStream(entry);

		Map<String, Object> map = Toml.read(manifestStream);

		System.out.println("Raw: " + map);

		ModManifest manifest = ModManifest.parse(map);

		System.out.println("Parsed: " + manifest);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject fabric = ModManifestConverter.convertToFabric(manifest);
		String json = gson.toJson(fabric);

		Path fabricModJson = Paths.get("data/fabric.mod.json");

		Files.write(fabricModJson, json.getBytes(StandardCharsets.UTF_8));

		System.out.println(json);
	}
}
