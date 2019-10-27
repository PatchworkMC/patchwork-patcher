package net.coderbot.patchwork.manifest.forge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.objectweb.asm.commons.Remapper;

public class AccessTransformerList {

	private List<AccessTransformerEntry> entries;
	public AccessTransformerList(List<AccessTransformerEntry> entries) {
		this.entries = entries;
	}

	public List<AccessTransformerEntry> getEntries() {
		return entries;
	}

	// TODO fails mysteriously when unable to remap
	public static AccessTransformerList parse(Path accessTransformer) throws Exception {
		List<String> lines = Files.readAllLines(accessTransformer);
		Map<String, String> ats = new HashMap<>(); // map of AT classes and fields/methods
		for(String line : lines) {
			// Put everything into the map. Changes package "." to folder "/". "\\" needed to escape
			// regex.
			if(line.startsWith("#"))
				continue;
			if(line.length() == 0)
				continue; // fixme hack
			String[] split = line.replaceAll("\\.", "/").split(" ");
			ats.put(split[1], split[2]);
		}
		// Set up our mappings

		//		TinyRemapper voldeToOfficalTiny = TinyRemapper.newRemapper()
		//												  .withMappings(voldeToOfficialProvider)
		//												  .rebuildSourceFilenames(true)
		//												  .ignoreFieldDesc(true)
		//												  .build();
		//		TinyRemapper officialToIntermediaryTiny = TinyRemapper.newRemapper()
		//														  .withMappings(officialToIntermediary)
		//														  .rebuildSourceFilenames(true)
		//														  .ignoreFieldDesc(true)
		//														  .build();
		// TODO is this necessary?
		//		voldeToOfficalTiny.readClassPath(Paths.get("data/1.14.4+srg.jar"));
		//		officialToIntermediaryTiny.readClassPath(Paths.get("data/1.14.4+official.jar"));
		//
		//		Remapper voldeToOfficialRemapper = voldeToOfficalTiny.getRemapper();
		//		Remapper officialToIntermedirayRemapper = officialToIntermediaryTiny.getRemapper();

		// for every AT the mod uses
		List<AccessTransformerEntry> entries = new ArrayList<>();
		for(Map.Entry entry : ats.entrySet()) {
			entries.add(new AccessTransformerEntry(
					((String) entry.getKey()), ((String) entry.getValue())));
		}
		return new AccessTransformerList(entries);
	}

	public AccessTransformerList remap(Remapper remapper) {
		List<AccessTransformerEntry> newEntries = new ArrayList<>();
		entries.forEach(e -> newEntries.add(e.remap(remapper)));
		entries.clear();
		entries.addAll(newEntries);
		return this;
	}
}
