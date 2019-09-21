package net.coderbot.patchwork;

import com.electronwill.toml.Toml;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.coderbot.patchwork.manifest.converter.ModManifestConverter;
import net.coderbot.patchwork.manifest.forge.ModManifest;
import net.coderbot.patchwork.voldemap.Tsrg;
import net.coderbot.patchwork.voldemap.TsrgClass;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.FieldEntry;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

		Mappings intermediary = MappingsProvider.readTinyMappings(new FileInputStream(new File("data/1.14.4.tiny")));

		HashMap<String, HashMap<String, String>> fieldDescriptions = new HashMap<>();

		for(FieldEntry fieldEntry: intermediary.getFieldEntries()) {
			EntryTriple official = fieldEntry.get("official");

			fieldDescriptions.computeIfAbsent(official.getOwner(), name -> new HashMap<>()).put(official.getName(), official.getDesc());
		}

		List<TsrgClass> classes = Tsrg.readMappings(new FileInputStream(new File("data/joined.tsrg")));

		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("data/mcp-1.14.4.tiny")));

		writer.write("v1\tofficial\tmcp\n");

		for(TsrgClass clazz: classes) {
			writer.write("CLASS\t" + clazz.getOfficial() + "\t" + clazz.getMcp() + "\n");

			HashMap<String, String> fieldToDescription = fieldDescriptions.get(clazz.getOfficial());

			for(TsrgClass.Entry field: clazz.getFields()) {
				String description = fieldToDescription.get(field.getOfficial());

				writer.write("FIELD\t" + clazz.getOfficial() + "\t" +  description + "\t" + field.getOfficial() + "\t" + field.getMcp() + "\n");
			}

			for(TsrgClass.DescribedEntry method: clazz.getMethods()) {
				writer.write("METHOD\t" + clazz.getOfficial() + "\t" +  method.getDescription() + "\t" + method.getOfficial() + "\t" +  method.getMcp() + "\n");
			}
		}

		writer.close();
	}
}
