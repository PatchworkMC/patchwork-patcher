package com.patchworkmc.mapping.remapper;

import java.util.HashMap;

import net.fabricmc.tinyremapper.IMappingProvider;

/**
 * Remaps classes, methods, and fields based on the assumption that names are never repeated in the source mappings.
 */
public class NaiveRemapper {
	private final HashMap<String, String> classes = new HashMap<>();
	private final HashMap<String, String> methods = new HashMap<>();
	private final HashMap<String, String> fields = new HashMap<>();

	public NaiveRemapper(IMappingProvider mappings) {
		mappings.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				if (classes.get(srcName) != null) throw new IllegalArgumentException("Duplicated class name " + srcName);
				classes.put(srcName, dstName);
			}

			@Override
			public void acceptMethod(IMappingProvider.Member method, String dstName) {
				if (classes.get(method.name) != null) throw new IllegalArgumentException("Duplicated method name " + method.name);
				methods.put(method.name, dstName);
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
				if (classes.get(field.name) != null) throw new IllegalArgumentException("Duplicated field name " + field.name);
				fields.put(field.name, dstName);
			}
		});
	}

	public String getClass(String volde) {
		return classes.getOrDefault(volde, volde);
	}

	public String getMethod(String volde) {
		return methods.getOrDefault(volde, volde);
	}

	public String getField(String volde) {
		return fields.getOrDefault(volde, volde);
	}
}
