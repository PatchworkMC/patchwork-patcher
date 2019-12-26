package com.patchworkmc.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.FieldEntry;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.tinyremapper.IMappingProvider;

public class TsrgMappings implements IMappingProvider {
	List<TsrgClass<Mapping>> classes;

	public TsrgMappings(List<TsrgClass<RawMapping>> classes, Mappings reference, String officialName) {
		this(classes, getFieldDescriptions(reference, officialName));
	}

	public TsrgMappings(List<TsrgClass<RawMapping>> unpairedClasses, Map<String, Map<String, String>> fieldDescriptions) {
		classes = new ArrayList<>();

		for (TsrgClass<RawMapping> unpaired : unpairedClasses) {
			TsrgClass<Mapping> paired = new TsrgClass<>(unpaired.getOfficial(), unpaired.getMapped());

			Map<String, String> fieldToDescription = fieldDescriptions.get(unpaired.getOfficial());

			if (fieldToDescription == null && unpaired.getFields().size() != 0) {
				throw new IllegalArgumentException("Provided field descriptions is missing descriptions for the class " + unpaired.getOfficial() + "(MCP Name: " + unpaired.getMapped() + ")");
			}

			for (RawMapping field : unpaired.getFields()) {
				String description = fieldToDescription.get(field.getOfficial());

				paired.addField(new Mapping(field.getOfficial(), field.getMapped(), description));
			}

			for (Mapping method : unpaired.getMethods()) {
				paired.addMethod(method);
			}

			classes.add(paired);
		}
	}

	private static HashMap<String, Map<String, String>> getFieldDescriptions(Mappings reference, String officialName) {
		if (!reference.getNamespaces().contains(officialName)) {
			throw new IllegalArgumentException("Provided mappings did not contain the namespace " + officialName);
		}

		HashMap<String, Map<String, String>> fieldDescriptions = new HashMap<>();

		for (FieldEntry fieldEntry : reference.getFieldEntries()) {
			EntryTriple official = fieldEntry.get(officialName);

			fieldDescriptions.computeIfAbsent(official.getOwner(), name -> new HashMap<>()).put(official.getName(), official.getDesc());
		}

		return fieldDescriptions;
	}

	public void load(MappingAcceptor out) {
		for (TsrgClass<Mapping> clazz : classes) {
			out.acceptClass(clazz.getOfficial(), clazz.getMapped());

			for (Mapping field : clazz.getFields()) {
				Member member = new Member(clazz.getOfficial(), field.getOfficial(), field.getDescription());

				out.acceptField(member, field.getMapped());
			}

			for (Mapping method : clazz.getMethods()) {
				Member member = new Member(clazz.getOfficial(), method.getOfficial(), method.getDescription());

				out.acceptMethod(member, method.getMapped());
			}
		}
	}

	public String writeTiny(String finalNamespace) {
		StringBuilder tiny = new StringBuilder();

		tiny.append("v1\tofficial\t");
		tiny.append(finalNamespace);
		tiny.append('\n');

		for (TsrgClass<Mapping> clazz : classes) {
			tiny.append("CLASS\t");
			tiny.append(clazz.getOfficial());
			tiny.append('\t');
			tiny.append(clazz.getMapped());
			tiny.append('\n');

			for (Mapping field : clazz.getFields()) {
				tiny.append("FIELD\t");
				tiny.append(clazz.getOfficial());
				tiny.append('\t');
				tiny.append(field.getDescription());
				tiny.append('\t');
				tiny.append(field.getOfficial());
				tiny.append('\t');
				tiny.append(field.getMapped());
				tiny.append('\n');
			}

			for (Mapping method : clazz.getMethods()) {
				tiny.append("METHOD\t");
				tiny.append(clazz.getOfficial());
				tiny.append('\t');
				tiny.append(method.getDescription());
				tiny.append('\t');
				tiny.append(method.getOfficial());
				tiny.append('\t');
				tiny.append(method.getMapped());
				tiny.append('\n');
			}
		}

		return tiny.toString();
	}
}
