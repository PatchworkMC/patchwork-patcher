package net.coderbot.patchwork.tsrg;

import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassRemapper extends Remapper {
	private final Map<String, String> classMapping;

	private ClassRemapper() {
		classMapping = new HashMap<>();
	}

	public static ClassRemapper officialToMcp(List<TsrgClass> classes) {
		ClassRemapper remapper = new ClassRemapper();

		for(TsrgClass clazz: classes) {
			remapper.classMapping.put(clazz.getOfficial(), clazz.getMapped());
		}

		return remapper;
	}

	public static ClassRemapper mcpToOfficial(List<TsrgClass> classes) {
		ClassRemapper remapper = new ClassRemapper();

		for(TsrgClass clazz: classes) {
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
