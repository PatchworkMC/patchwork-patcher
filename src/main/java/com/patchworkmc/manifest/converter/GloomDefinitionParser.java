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

public class GloomDefinitionParser {
	public static GloomDefinitions parse(AccessTransformerList list, FieldDescriptorProvider provider) {
		HashMap<String, MemberHolder> members = new HashMap<>();

		for (AccessTransformerEntry entry : list.getEntries()) {
			String className = entry.getClassName().replace('.', '/');
			MemberHolder holder = members.computeIfAbsent(className, s -> new MemberHolder());

			String memberName = entry.getMemberName();

			if (entry.isField()) {
				String descriptor = provider.getDescriptor(className, memberName);

				if (descriptor == null) {
					throw new IllegalStateException("Missing descriptor for " + className + "." + memberName);
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
