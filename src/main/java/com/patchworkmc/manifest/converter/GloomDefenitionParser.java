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
	public static GloomDefinitions parse(AccessTransformerList list, ManifestRemapper remapper) {
		HashMap<String, MemberHolder> memberMap = new HashMap<>();

		for (AccessTransformerEntry entry : list.getEntries()) {
			MemberHolder holder = memberMap.computeIfAbsent(entry.getClassName(), s -> new MemberHolder());

			String memberName = entry.getMemberName();

			if (entry.isField()) {
				String descriptor = remapper.remapMemberDescription(remapper.getNaiveRemapper().getFieldDescSpecial(memberName));
				holder.fields.add(new SelfMember(memberName, descriptor));
			} else {
				String descriptor = entry.getDescriptor();
				holder.methods.add(new SelfMember(memberName, descriptor));
			}
		}

		HashSet<ClassDefinition> classDefs = new HashSet<>();

		for (Map.Entry<String, MemberHolder> entry : memberMap.entrySet()) {
			MemberHolder holder = entry.getValue();
			classDefs.add(new ClassDefinition(entry.getKey(), Collections.emptySet(), holder.fields, holder.methods, new HashSet<>(holder.fields), Collections.emptySet(), Collections.emptySet()));
		}

		return new GloomDefinitions(classDefs);
	}

	private static class MemberHolder {
		private final HashSet<SelfMember> fields = new HashSet<>();
		private final HashSet<SelfMember> methods = new HashSet<>();
	}
}
