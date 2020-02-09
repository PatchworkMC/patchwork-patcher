package com.patchworkmc.manifest.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
		HashMap<String, Set<SelfMember>> classFields = new HashMap<>();
		HashMap<String, Set<SelfMember>> classMethods = new HashMap<>();
		HashSet<String> classes = new HashSet<>();

		for (AccessTransformerEntry entry : list.getEntries()) {
			classes.add(entry.getClassName());

			if (entry.isField()) {
				populateMapForEntry(classFields, entry);
			} else {
				populateMapForEntry(classMethods, entry);
			}
		}

		HashSet<ClassDefinition> classDefs = new HashSet<>();

		for (String className : classes) {
			Set<SelfMember> fieldSet = classFields.get(className);
			Set<SelfMember> methodSet = classMethods.get(className);
			classDefs.add(new ClassDefinition(className, Collections.emptySet(), fieldSet, methodSet, new HashSet<>(fieldSet), Collections.emptySet(), Collections.emptySet()));
		}

		return new GloomDefinitions(classDefs);
	}

	private static void populateMapForEntry(HashMap<String, Set<SelfMember>> map, AccessTransformerEntry at) {
		Set<SelfMember> entrySet = map.computeIfAbsent(at.getClassName(), key -> new HashSet<>());
		entrySet.add(new SelfMember(at.getMemberName(), at.getDescriptor()));
	}
}
