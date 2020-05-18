package com.patchworkmc.mapping;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.tinyremapper.IMappingProvider;

/**
 * A lazily loading representation of all Minecraft names and descriptors.
 */
public class IntermediaryHolder {
	private final HashMap<String, Map<String, Member>> mappings = new HashMap<>();

	private final IMappingProvider inverted;

	private boolean loaded = false;

	public IntermediaryHolder(IMappingProvider inverted) {
		this.inverted = inverted;
	}

	public Map<String, Member> getMappings(String owner) {
		// We load lazily here so that if this class isn't needed we can save some time and memory
		if (!this.loaded) {
			inverted.load(new Acceptor());
			loaded = true;
		}

		return mappings.get(owner);
	}

	// kotlin-style object to hold fields.
	public class Member {
		public final String name;
		public final String descriptor;
		public final boolean isField;

		public Member(String name, String descriptor, boolean isField) {
			this.name = name;
			this.descriptor = descriptor;
			this.isField = isField;
		}
	}

	private class Acceptor implements IMappingProvider.MappingAcceptor {
		@Override
		public void acceptClass(String srcName, String dstName) {
			mappings.put(srcName, new HashMap<>());
		}

		@Override
		public void acceptMethod(IMappingProvider.Member method, String dstName) {
			mappings.computeIfAbsent(method.owner, s -> new HashMap<>()).put(method.name + method.desc, new Member(method.name, method.desc, false));
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
			mappings.computeIfAbsent(field.owner, s -> new HashMap<>()).put(field.name, new Member(field.name, field.desc, false));
		}
	}
}
