package com.patchworkmc.mapping.remapper;

import java.util.HashMap;
import java.util.HashSet;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.manifest.accesstransformer.v2.exception.MissingMappingException;

import com.patchworkmc.Patchwork;
import com.patchworkmc.mapping.MappingAssertions;

public class PatchworkRemapper {
	private static final boolean DEBUG = false;

	private final Naive naiveRemapper;

	private final HashMap<String, String> classes = new HashMap<>();
	// method.name + method.desc for methods, field.name for fields
	private final HashMap<String, HashMap<String, String>> memberMap = new HashMap<>();

	private final HashSet<String> blacklistedMethods = new HashSet<>();

	public PatchworkRemapper(IMappingProvider mappings) {
		this.naiveRemapper = new Naive();

		mappings.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				memberMap.computeIfAbsent(srcName, s -> new HashMap<>());

				if (classes.get(srcName) == null) {
					classes.put(srcName, dstName);
				} else {
					throw new IllegalArgumentException("Duplicated class name " + srcName);
				}
			}

			@Override
			public void acceptMethod(IMappingProvider.Member method, String dstName) {
				memberMap.computeIfAbsent(method.owner, s -> new HashMap<>())
					.put(method.name + method.desc, dstName);

				if (!method.name.startsWith("func_")) {
					return;
				}

				if (blacklistedMethods.contains(method.name)) {
					if (DEBUG) {
						Patchwork.LOGGER.debug("Another duplicated method mapping for %s (proposed %s)", method.name, dstName);
					}

					return;
				}

				// Have to include some exceptions
				String presentName = naiveRemapper.methods.get(method.name);

				if (presentName == null) {
					naiveRemapper.methods.put(method.name, dstName);
				} else if (!presentName.equals(dstName)) {
					blacklistedMethods.add(method.name);
					naiveRemapper.methods.remove(method.name);

					if (DEBUG) {
						Patchwork.LOGGER.debug("Duplicated method mapping for %s (proposed %s, but already mapped to %s!)", method.name, dstName, presentName);
					}
				}
			}

			@Override
			public void acceptMethodArg(IMappingProvider.Member method, int lvIndex, String dstName) {
				// NO-OP
			}

			@Override
			public void acceptMethodVar(IMappingProvider.Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
				// NO-OP
			}

			@Override
			public void acceptField(IMappingProvider.Member field, String dstName) {
				memberMap.computeIfAbsent(field.owner, s -> new HashMap<>()).put(field.name, dstName);

				if (!field.name.startsWith("field_")) {
					return;
				}

				String presentName = naiveRemapper.fields.get(field.name);

				if (presentName == null) {
					naiveRemapper.fields.put(field.name, dstName);
				} else if (!presentName.equals(dstName)) {
					throw new IllegalArgumentException(String.format("Duplicated field mapping for %s (proposed %s, but key already mapped to %s!)", field.name, dstName, presentName));
				}
			}
		});
	}

	public String getMethod(String owner, String name, String descriptor) throws MissingMappingException {
		HashMap<String, String> classMembers = this.memberMap.get(owner.replace('.', '/'));
		MappingAssertions.assertClassExists(classMembers, name);

		if (name.equals("<init>")) {
			return "<init>";
		}

		String result = classMembers.get(name + descriptor);

		MappingAssertions.assertMethodExists(result, owner, name+descriptor);

		return result;
	}

	public String getField(String owner, String name) throws MissingMappingException {
		HashMap<String, String> classMembers = this.memberMap.get(owner.replace('.', '/'));
		MappingAssertions.assertClassExists(classMembers, owner);

		String result = classMembers.get(name);
		MappingAssertions.assertFieldExists(result, owner, name);

		return result;
	}

	public String getClass(String volde) throws MissingMappingException {
		String result = classes.get(volde);
		MappingAssertions.assertClassExists(result, volde);

		return result;
	}

	public Naive getNaiveRemapper() {
		return naiveRemapper;
	}

	/**
	 * Remaps classes, methods, and fields based on the assumption that names are never repeated in the source mappings,
	 * or that they duplicate directly in the target mappings. (i.e. 'valueOf' -> 'valueOf')
	 *
	 * <p>Mappings that are duplicated will be ignored after their first entry.</p>
	 */
	public class Naive {
		private final HashMap<String, String> methods = new HashMap<>();
		private final HashMap<String, String> fields = new HashMap<>();

		private Naive() {
		}

		/**
		 * @deprecated Because of java generics, inheritence, synthetics, and recompiling, Forge matches methods that override another method but change
		 *  return type T to class_XXX directly, but Fabric matches to the synthetic. Runtime mappings should be used here instead.
		 * @param volde
		 * @return
		 */
		@Deprecated
		public String getMethod(String volde) throws AmbiguousMappingException {
			if (!volde.startsWith("func_")) {
				throw new IllegalArgumentException("Cannot remap methods not starting with func_: " + volde);
			}

			if (blacklistedMethods.contains(volde)) {
				throw new AmbiguousMappingException("Cannot remap method name " + volde + " because that method name could map to multiple targets!");
			}

			return methods.getOrDefault(volde, volde);
		}

		public String getField(String volde) {
			if (!volde.startsWith("field_")) {
				throw new IllegalArgumentException("Cannot remap fields not starting with field_: " + volde);
			}

			return fields.getOrDefault(volde, volde);
		}

		public String getClass(String volde) {
			return classes.getOrDefault(volde, volde);
		}
	}
}
