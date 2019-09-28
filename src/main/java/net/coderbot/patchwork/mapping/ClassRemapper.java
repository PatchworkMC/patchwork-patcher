package net.coderbot.patchwork.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.commons.Remapper;

public class ClassRemapper extends Remapper {
	private final Map<String, String> classMapping;

	private ClassRemapper() {
		classMapping = new HashMap<>();
	}

	/**
	 * Remaps classes from official names to mapped (MCP / SRG) names.
	 * @param classes The list of mapping classes
	 * @return A class remapper based on the provided classes
	 */
	public static ClassRemapper officialToMapped(List<TsrgClass<Mapping>> classes) {
		ClassRemapper remapper = new ClassRemapper();

		for(TsrgClass clazz : classes) {
			remapper.classMapping.put(clazz.getOfficial(), clazz.getMapped());
		}

		return remapper;
	}

	public static ClassRemapper mappedToOfficial(List<TsrgClass<Mapping>> classes) {
		ClassRemapper remapper = new ClassRemapper();

		for(TsrgClass clazz : classes) {
			remapper.classMapping.put(clazz.getMapped(), clazz.getOfficial());
		}

		return remapper;
	}

	@Override
	public String map(String official) {

		String mapped = classMapping.get(official);

		return mapped != null ? mapped : super.map(official);
	}
}
