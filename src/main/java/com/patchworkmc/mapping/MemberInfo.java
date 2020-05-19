package com.patchworkmc.mapping;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.manifest.accesstransformer.v2.exception.MissingMappingException;

import com.patchworkmc.manifest.converter.accesstransformer.AccessTransformerConverter;

/**
 * A lazily loading representation of all Minecraft names and descriptors in the target mappings.
 * Used in {@link AccessTransformerConverter} to resolve descriptors for fields and resolve all names for wildcards.
 */
public class MemberInfo {
	private final HashMap<String, ClassMembers> mappings = new HashMap<>();

	private final IMappingProvider targetFirst;

	private boolean loaded = false;

	/**
	 * @param targetFirst the mappings, in the format {@code target -> <any>}.
	 */
	public MemberInfo(IMappingProvider targetFirst) {
		this.targetFirst = targetFirst;
	}

	public ClassMembers getMappings(String owner) throws MissingMappingException {
		// We load lazily here so that if this class isn't needed we can save some time and memory
		if (!this.loaded) {
			targetFirst.load(new Acceptor());
			loaded = true;
		}

		ClassMembers result = mappings.get(owner);
		MappingAssertions.assertClassExists(result, owner);
		return result;
	}

	/**
	 * POJO for {@link Member}s.
	 */
	public static class ClassMembers {
		public final Map<String, Member> fields = new HashMap<>();
		public final Map<String, Member> methods = new HashMap<>();
	}

	/**
	 * POJO to hold methods and fields.
	 */
	public static class Member {
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
			mappings.put(srcName, new ClassMembers());
		}

		@Override
		public void acceptMethod(IMappingProvider.Member method, String dstName) {
			mappings.computeIfAbsent(method.owner, s -> new ClassMembers()).methods.put(method.name + method.desc, new Member(method.name, method.desc, false));
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
			mappings.computeIfAbsent(field.owner, s -> new ClassMembers()).fields.put(field.name, new Member(field.name, field.desc, false));
		}
	}
}
