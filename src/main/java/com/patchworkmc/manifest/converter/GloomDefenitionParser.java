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

public class GloomDefenitionParser {
	private GloomDefenitionParser() {
		// NO-OP
	}

	public static GloomDefinitions parse(AccessTransformerList list) {
		HashMap<String, MemberHolder> definitionMap = new HashMap<>();

		for (AccessTransformerEntry entry : list.getEntries()) {
			MemberHolder classDefinition = new MemberHolder();
			definitionMap.put(entry.getClassName(), classDefinition);

			if (entry.isField()) {
				classDefinition.fields.add(new SelfMember(entry.getMemberName(), entry.getDescriptor()));
			} else {
				classDefinition.methods.add(new SelfMember(entry.getMemberName(), entry.getDescriptor()));
			}
		}

		HashSet<ClassDefinition> classDefs = new HashSet<>();

		for (Map.Entry<String, MemberHolder> entry : definitionMap.entrySet()) {
			MemberHolder classDefinition = entry.getValue();
			classDefs.add(new ClassDefinition(entry.getKey(), Collections.emptySet(), classDefinition.fields, classDefinition.methods, new HashSet<>(classDefinition.fields), Collections.emptySet(), Collections.emptySet()));
		}

		return new GloomDefinitions(classDefs);
	}

	private static class MemberHolder {
		private final HashSet<SelfMember> fields = new HashSet<>();
		private final HashSet<SelfMember> methods = new HashSet<>();
	}
}
