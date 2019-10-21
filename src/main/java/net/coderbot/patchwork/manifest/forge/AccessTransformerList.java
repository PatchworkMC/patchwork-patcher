package net.coderbot.patchwork.manifest.forge;

import net.coderbot.patchwork.mapping.TsrgMappings;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			IMappingProvider voldeToOfficialProvider,
			IMappingProvider officialToIntermediary) throws Exception {
		List<String> lines = Files.readAllLines(accessTransformer);
		Map<String, String> ats = new HashMap<>(); // map of AT classes and fields/methods
		for(String line : lines) {
			// Put everything into the map. Changes package "." to folder "/". "\\" needed to escape
			// regex.
			String[] split = line.replaceAll("\\.", "/").split(" ");
			ats.put(split[1], split[2]);
		}
		// Set up our mappings

		TinyRemapper voldeToOfficalTiny = TinyRemapper.newRemapper()
				.withMappings(voldeToOfficialProvider)
				.rebuildSourceFilenames(true)
				.build();
		TinyRemapper officialToIntermediaryTiny = TinyRemapper.newRemapper()
				.withMappings(officialToIntermediary)
				.rebuildSourceFilenames(true)
				.build();
		//TODO is this necessary?
		voldeToOfficalTiny.readClassPath(Paths.get("data/1.14.4+srg.jar"));
		officialToIntermediaryTiny.readClassPath(Paths.get("data/1.14.4+official.jar"));
		//Removing this causes mappings to sometimes fail for volde. TODO test official
		voldeToOfficalTiny.apply(null);
		officialToIntermediaryTiny.apply(null);

		Remapper voldeToOfficialRemapper;
		Remapper officialToIntermedirayRemapper;
		try {
			Field asmRemapper = voldeToOfficalTiny.getClass().getDeclaredField("remapper");
			asmRemapper.setAccessible(true);

			voldeToOfficialRemapper = (Remapper) asmRemapper.get(voldeToOfficalTiny);
			officialToIntermedirayRemapper = (Remapper) asmRemapper.get(officialToIntermediaryTiny);
		} catch(NoSuchFieldException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
		// for every AT the mod uses
		List<AccessTransformerEntry> entries = new ArrayList<>();
		for(Map.Entry entry : ats.entrySet()) {
			entries.add(new AccessTransformerEntry(((String) entry.getKey()),
					((String) entry.getValue()),
					voldeToOfficialRemapper,
					officialToIntermedirayRemapper));
		}
		return new AccessTransformerList(entries);
	}
}
