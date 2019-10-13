package net.coderbot.patchwork.manifest.forge;

import net.coderbot.patchwork.access.AccessTransformer;
import net.coderbot.patchwork.mapping.TsrgMappings;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.mappings.*;

public class AccessTransformerList {

	private List<AccessTransformerEntry> entries;
	public AccessTransformerList(List<AccessTransformerEntry> entries) {
		this.entries = entries;
	}

	public List<AccessTransformerEntry> getEntries() {
		return entries;
	}

	// TODO fails mysteriously when unable to remap
	public static AccessTransformerList parse(Path accessTransformer,
			TsrgMappings voldeToOfficialProvider,
			Mappings officialToIntermediary) throws Exception {
		List<String> lines = Files.readAllLines(accessTransformer);
		Map<String, String> ats = new HashMap<>(); // map of AT classes and fields/methods
		for(String line : lines) {
			// Put everything into the map. Changes package "." to folder "/". "\\" needed to escape
			// regex.
			String[] split = line.replaceAll("\\.", "/").split(" ");
			ats.put(split[1], split[2]);
		}
		// the mappings for converting volde to official
		Mappings voldeToOfficial = MappingsProvider.readTinyMappings(
				new ByteArrayInputStream(voldeToOfficialProvider.writeTiny("srg").getBytes()));
		// for every AT the mod uses
		List<AccessTransformerEntry> entries = new ArrayList<>();
		for(Map.Entry entry : ats.entrySet()) {
			entries.add(new AccessTransformerEntry(((String) entry.getKey()),
					((String) entry.getValue()),
					voldeToOfficial,
					officialToIntermediary));
		}
		return new AccessTransformerList(entries);
	}
}
