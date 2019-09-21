package net.coderbot.patchwork;

import net.coderbot.patchwork.tsrg.*;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Patchwork {
	public static void main(String[] args) throws Exception {
		/*ZipFile file = new ZipFile(new File("data/BiomesOPlenty-1.14.4-9.0.0.253-universal.jar"));

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

		System.out.println(json);*/

		Mappings intermediary = MappingsProvider.readTinyMappings(new FileInputStream(new File("data/mappings/intermediary-1.14.4.tiny")));
		List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(new FileInputStream(new File("data/mappings/voldemap-1.14.4.tsrg")));

		TsrgMappings mappings = new TsrgMappings(classes, intermediary, "official");
		String tiny = mappings.writeTiny("srg");

		Files.write(Paths.get("data/mappings/voldemap-1.14.4.tiny"), tiny.getBytes(StandardCharsets.UTF_8));

		// String bridged = VoldeBridge.bridgeMappings(classes, intermediary);

		// Files.write(Paths.get("data/mappings/voldemap-intermediary-1.14.4.tiny"), bridged.getBytes(StandardCharsets.UTF_8));
	}
}
