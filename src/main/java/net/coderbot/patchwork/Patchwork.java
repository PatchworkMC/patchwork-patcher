package net.coderbot.patchwork;

import net.coderbot.patchwork.mapping.*;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.tinyremapper.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

		IMappingProvider intermediaryMappings = TinyUtils.createTinyMappingProvider(Paths.get("data/mappings/intermediary-1.14.4.tiny"), "official", "intermediary");

		TsrgMappings mappings = new TsrgMappings(classes, intermediary, "official");
		String tiny = mappings.writeTiny("srg");

		Files.write(Paths.get("data/mappings/voldemap-1.14.4.tiny"), tiny.getBytes(StandardCharsets.UTF_8));

		// String mod = "BiomesOPlenty-1.14.4-9.0.0.253-universal";
		String mod = "voyage-1.0.0";

		System.out.println("Remapping Minecraft (official -> srg)");
		remap(mappings, Paths.get("data/1.14.4+official.jar"), Paths.get("data/1.14.4+srg.jar"));

		System.out.println("Remapping " + mod + " (srg -> official)");
		remap(new InvertedTsrgMappings(mappings), Paths.get("data/" + mod + "+srg.jar"), Paths.get("data/" + mod + "+official.jar"), Paths.get("data/1.14.4+srg.jar"));

		System.out.println("Remapping " + mod + " (official -> intermediary)");
		remap(intermediaryMappings, Paths.get("data/" + mod + "+official.jar"), Paths.get("data/" + mod + "+intermediary.jar"), Paths.get("data/1.14.4+official.jar"));
	}

	private static void remap(IMappingProvider mappings, Path input, Path output, Path... classpath) throws IOException {
		TinyRemapper remapper = TinyRemapper
				.newRemapper()
				.withMappings(mappings)
				.rebuildSourceFilenames(true)
				.build();


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
