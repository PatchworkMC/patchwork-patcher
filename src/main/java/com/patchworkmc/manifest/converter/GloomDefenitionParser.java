package com.patchworkmc.manifest.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.github.fukkitmc.gloom.definitions.ClassDefinition;
import io.github.fukkitmc.gloom.definitions.GloomDefinitions;
import io.github.fukkitmc.gloom.definitions.SelfMember;

import com.patchworkmc.manifest.accesstransformer.AccessTransformerEntry;
import com.patchworkmc.manifest.accesstransformer.AccessTransformerList;
import com.patchworkmc.mapping.remapper.ManifestRemapper;

public class GloomDefenitionParser {
	public static GloomDefinitions parse(AccessTransformerList list, FieldDescriptorProvider provider) {
		HashMap<String, MemberHolder> members = new HashMap<>();

		for (AccessTransformerEntry entry : list.getEntries()) {
			MemberHolder holder = members.computeIfAbsent(entry.getClassName(), s -> new MemberHolder());

			String memberName = entry.getMemberName();

			if (entry.isField()) {
				String descriptor = provider.getDescriptor(entry.getClassName(), memberName);

				if (descriptor == null) {
					throw new IllegalStateException("Missing descriptor for " + entry.getClassName() + "." + memberName);
				}

				holder.fields.add(new SelfMember(memberName, descriptor));
			} else {
				String descriptor = entry.getDescriptor();
				holder.methods.add(new SelfMember(memberName, descriptor));
			}
		}

		HashSet<ClassDefinition> classDefinitions = new HashSet<>();

		for (Map.Entry<String, MemberHolder> entry : members.entrySet()) {
			MemberHolder holder = entry.getValue();
			classDefinitions.add(new ClassDefinition(entry.getKey(), Collections.emptySet(), holder.fields, holder.methods, new HashSet<>(holder.fields), Collections.emptySet(), Collections.emptySet()));
		}

		return new GloomDefinitions(classDefinitions);
	}

	private static class MemberHolder {
		private final HashSet<SelfMember> fields = new HashSet<>();
		private final HashSet<SelfMember> methods = new HashSet<>();
	}
}
