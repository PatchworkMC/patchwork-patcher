package net.patchworkmc.patcher.mapping;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.patcher.mapping.remapper.ClassRemapper;

/**
 * Given a {@link TsrgMappings} (official -> srg) and an {@link IMappingProvider} (official -> intermediary), this class
 * provides an {@link IMappingProvider} that maps from srg to intermediary.
 */
public class BridgedMappings implements IMappingProvider {
	private IMappingProvider intermediary;
	private ClassRemapper remapper;
	private Map<String, TsrgClass<Mapping>> officialToTsrg;

	public BridgedMappings(TsrgMappings mappings, IMappingProvider intermediary) {
		this.intermediary = intermediary;
		this.remapper = ClassRemapper.officialToMapped(mappings.classes);
		this.officialToTsrg = new HashMap<>(mappings.classes.size());

		for (TsrgClass<Mapping> clazz : mappings.classes) {
			officialToTsrg.put(clazz.getOfficial(), clazz);
		}
	}

	private static void missingMemberMapping(String kind, Member official, String intermediary, String srgClassName) {
		throw new IllegalArgumentException("srg is missing a " + kind + " mapping: official = " + describe(official) + ", intermediary = ?:" + intermediary + "(descriptor = ?), srg = " + srgClassName + ":missing(desc: missing)");
	}

	private static String describe(Member entry) {
		return entry.owner + ":" + entry.name + "(descriptor = \"" + entry.desc + "\")";
	}

	@Override
	public void load(MappingAcceptor out) {
		MappingAcceptor acceptor = new MappingFilter(remapper, officialToTsrg, out);

		intermediary.load(acceptor);
	}

	private static class MappingFilter implements IMappingProvider.MappingAcceptor {
		private ClassRemapper remapper;
		private Map<String, TsrgClass<Mapping>> officialToTsrg;
		private MappingAcceptor target;

		private MappingFilter(ClassRemapper remapper, Map<String, TsrgClass<Mapping>> officialToTsrg, MappingAcceptor target) {
			this.remapper = remapper;
			this.officialToTsrg = officialToTsrg;
			this.target = target;
		}

		@Override
		public void acceptClass(String official, String intermediary) {
			TsrgClass<Mapping> clazz = officialToTsrg.get(official);

			if (clazz == null) {
				throw new IllegalArgumentException("Missing tsrg mappings for class " + intermediary + " (official: " + official + ")");
			}

			target.acceptClass(clazz.getMapped(), intermediary);
		}

		@Override
		public void acceptMethod(Member official, String intermediary) {
			TsrgClass<Mapping> clazz = officialToTsrg.get(official.owner);

			if (clazz == null) {
				throw new IllegalArgumentException("Missing tsrg mappings for class " + intermediary + " (official: " + official + ")");
			}

			Mapping method = clazz.getMethod(official.name, official.desc);

			if (method == null) {
				missingMemberMapping("method", official, intermediary, clazz.getMapped());
			}

			String mappedDesc = remapper.mapMethodDesc(official.desc);
			IMappingProvider.Member fieldMember = new IMappingProvider.Member(clazz.getMapped(), method.getMapped(), mappedDesc);

			target.acceptMethod(fieldMember, intermediary);
		}

		@Override
		public void acceptMethodArg(Member method, int lvIndex, String dstName) {
			// No-op for now
		}

		@Override
		public void acceptMethodVar(Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
			// No-op for now
		}

		@Override
		public void acceptField(Member official, String intermediary) {
			TsrgClass<Mapping> clazz = officialToTsrg.get(official.owner);

			if (clazz == null) {
				throw new IllegalArgumentException("Missing tsrg mappings for class " + intermediary + " (official: " + official + ")");
			}

			Mapping field = clazz.getField(official.name);

			if (field == null) {
				missingMemberMapping("field", official, intermediary, clazz.getMapped());
			}

			String mappedDesc = remapper.mapDesc(official.desc);
			IMappingProvider.Member fieldMember = new IMappingProvider.Member(clazz.getMapped(), field.getMapped(), mappedDesc);

			target.acceptField(fieldMember, intermediary);
		}
	}
}
