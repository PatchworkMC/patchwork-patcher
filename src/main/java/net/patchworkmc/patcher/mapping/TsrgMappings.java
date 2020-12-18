package net.patchworkmc.patcher.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.tinyremapper.IMappingProvider;

public class TsrgMappings implements IMappingProvider {
	List<TsrgClass<Mapping>> classes;

	public TsrgMappings(List<TsrgClass<RawMapping>> classes, IMappingProvider reference) {
		this(classes, getFieldDescriptions(reference));
	}

	public TsrgMappings(List<TsrgClass<RawMapping>> unpairedClasses, Map<String, Map<String, String>> fieldDescriptions) {
		classes = new ArrayList<>();

		for (TsrgClass<RawMapping> unpaired : unpairedClasses) {
			TsrgClass<Mapping> paired = new TsrgClass<>(unpaired.getOfficial(), unpaired.getMapped());

			Map<String, String> fieldToDescription = fieldDescriptions.get(unpaired.getOfficial());

			if (fieldToDescription == null && unpaired.getFields().size() != 0) {
				throw new IllegalArgumentException("Provided field descriptions is missing descriptions for the class " + unpaired.getOfficial() + " (MCP Name: " + unpaired.getMapped() + ")");
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

	private static HashMap<String, Map<String, String>> getFieldDescriptions(IMappingProvider reference) {
		HashMap<String, Map<String, String>> fieldDescriptions = new HashMap<>();

		reference.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				// No-op
			}

			@Override
			public void acceptMethod(Member method, String dstName) {
				// No-op
			}

			@Override
			public void acceptMethodArg(Member method, int lvIndex, String dstName) {
				// No-op
			}

			@Override
			public void acceptMethodVar(Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
				// No-op
			}

			@Override
			public void acceptField(Member field, String dstName) {
				fieldDescriptions.computeIfAbsent(field.owner, name -> new HashMap<>()).put(field.name, field.desc);
			}
		});

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
}
