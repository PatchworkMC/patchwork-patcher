package com.patchworkmc.mapping.remapper;

import java.util.HashMap;

import net.fabricmc.tinyremapper.IMappingProvider;

public class SimpleBridgedRemapper {
	private final HashMap<String, String> classes = new HashMap<>();
	private final HashMap<String, String> methods = new HashMap<>();
	private final HashMap<String, String> fields = new HashMap<>();

	public SimpleBridgedRemapper(IMappingProvider bridged) {
		bridged.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				classes.put(srcName, dstName);
			}

			@Override
			public void acceptMethod(IMappingProvider.Member method, String dstName) {
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
