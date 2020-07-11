package net.patchworkmc.patcher.mapping;

import net.patchworkmc.patcher.mapping.remapper.ClassRemapper;

import net.fabricmc.tinyremapper.IMappingProvider;

public class InvertedTsrgMappings implements IMappingProvider {
	private TsrgMappings mappings;
	private ClassRemapper remapper;

	public InvertedTsrgMappings(TsrgMappings mappings) {
		this.mappings = mappings;
		this.remapper = ClassRemapper.officialToMapped(mappings.classes);
	}

	public void load(MappingAcceptor out) {
		for (TsrgClass<Mapping> clazz : mappings.classes) {
			out.acceptClass(clazz.getMapped(), clazz.getOfficial());

			for (Mapping field : clazz.getFields()) {
				String description = remapper.mapDesc(field.getDescription());

				Member member = new Member(clazz.getMapped(), field.getMapped(), description);

				out.acceptField(member, field.getOfficial());
			}

			for (Mapping method : clazz.getMethods()) {
				String description = remapper.mapMethodDesc(method.getDescription());

				Member member = new Member(clazz.getMapped(), method.getMapped(), description);

				out.acceptMethod(member, method.getOfficial());
			}
		}
	}

	/**
	 * @return the original mappings used to created these inverted mappings
	 */
	public TsrgMappings getOriginal() {
		return mappings;
	}
}
