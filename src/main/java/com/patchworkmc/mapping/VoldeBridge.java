package com.patchworkmc.mapping;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.mappings.ClassEntry;
import net.fabricmc.mappings.EntryTriple;
import net.fabricmc.mappings.FieldEntry;
import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MethodEntry;

public class VoldeBridge {
	/**
	 * Creates srg -> intermediary mappings from official -> intermediary and official -> srg mappings.
	 *
	 * @param intermediaryMappings
	 * @param classes
	 */
	public static String bridgeMappings(List<TsrgClass<?>> classes, Mappings intermediaryMappings) {
		// First, create an official -> src type descriptor mapper
		// ClassRemapper remapper = ClassRemapper.officialToMcp(classes);

		if (!intermediaryMappings.getNamespaces().contains("official")) {
			throw new IllegalArgumentException("intermediary mappings must contain an official column");
		}

		if (!intermediaryMappings.getNamespaces().contains("intermediary")) {
			throw new IllegalArgumentException("intermediary mappings must contain an intermediary column");
		}

		// First, structure the intermediary mappings
		// Create official -> intermediary maps

		Map<String, TinyClass> intermediaryClasses = new HashMap<>();

		for (ClassEntry entry : intermediaryMappings.getClassEntries()) {
			String official = entry.get("official");
			String intermediary = entry.get("intermediary");

			intermediaryClasses.put(official, new TinyClass(intermediary));
		}

		for (FieldEntry entry : intermediaryMappings.getFieldEntries()) {
			EntryTriple official = entry.get("official");
			EntryTriple intermediary = entry.get("intermediary");

			TinyClass parent = intermediaryClasses.get(official.getOwner());

			if (parent == null) {
				throw new IllegalArgumentException("detected an orphan field: official = " + official + ", intermediary = " + intermediary);
			}

			parent.fields.put(official.getName(), new TinyEntry(intermediary.getName(), official.getDesc()));
		}

		for (MethodEntry entry : intermediaryMappings.getMethodEntries()) {
			EntryTriple official = entry.get("official");
			EntryTriple intermediary = entry.get("intermediary");

			TinyClass parent = intermediaryClasses.get(official.getOwner());

			if (parent == null) {
				throw new IllegalArgumentException("detected an orphan method: official = " + official + ", intermediary = " + intermediary);
			}

			parent.methods.computeIfAbsent(official.getName(), name -> new HashMap<>()).put(official.getDesc(), new TinyEntry(intermediary.getName(), official.getDesc()));
		}

		// Now that the mappings are organized, we can export tsrg -> tiny

		StringWriter stringWriter = new StringWriter();
		// BufferedWriter writer = new BufferedWriter(stringWriter);

		/*try {
			writer.write("v1\tsrg\tintermediary\n");

			for(TsrgClass clazz: classes) {
				TinyClass tiny = intermediaryClasses.get(clazz.getOfficial());

				if(tiny == null) {
					throw new IllegalArgumentException("intermediary is missing a mapping:
		"+clazz.getOfficial()+" / "+clazz.getMcp());
				}

				writer.write("CLASS\t" + clazz.getMcp() + "\t" + tiny.intermediary + "\n");

				for(TsrgClass.Entry field: clazz.getFields()) {
					TinyEntry tinyField = tiny.fields.get(field.getOfficial());

					if(tinyField == null) {
						System.err.println("intermediary is missing a field mapping:
		"+clazz.getOfficial()+":"+field.getOfficial()+" / "+clazz.getMcp()+":"+field.getMcp());
						continue;
					}

					String mappedDesc = remapper.mapDesc(tinyField.officialDescriptor);

					writer.write("FIELD\t" + clazz.getMcp() + "\t" +  mappedDesc + "\t" +
		field.getMcp() + "\t" + tinyField.intermediary + "\n");
				}

				for(TsrgClass.DescribedEntry method: clazz.getMethods()) {
					HashMap<String, TinyEntry> methodMap = tiny.methods.get(method.getOfficial());

					if(methodMap == null) {
						System.err.println("intermediary is missing a method mapping:
		"+clazz.getOfficial()+":"+method.getOfficial()+" / "+clazz.getMcp()+":"+method.getMcp());
						continue;
					}

					TinyEntry tinyMethod = methodMap.get(method.getDescription());

					if(tinyMethod == null) {
						System.err.println("intermediary is missing a method mapping:
		"+clazz.getOfficial()+":"+method.getOfficial()+"["+method.getDescription()+"] /
		"+clazz.getMcp()+":"+method.getMcp()); continue;
					}

					String mappedDesc = remapper.mapMethodDesc(tinyMethod.officialDescriptor);

					writer.write("METHOD\t" + clazz.getMcp() + "\t" +  mappedDesc + "\t" +
		method.getMcp() + "\t" + tinyMethod.intermediary + "\n");
				}
			}

			writer.flush();
		} catch(IOException e) {
			// Should never happen, string writing is infallible

			throw new IllegalStateException("Got an impossible IOException!", e);
		}*/

		return stringWriter.toString();
	}

	private static class TinyClass {
		
		// Value is never used, but is assigned. Probably will be used in the future
		@SuppressWarnings("unused")
		String intermediary;

		Map<String, TinyEntry> fields;

		// (official -> (descriptor -> intermediary))
		Map<String, HashMap<String, TinyEntry>> methods;

		TinyClass(String intermediary) {
			this.intermediary = intermediary;

			this.fields = new HashMap<>();
			this.methods = new HashMap<>();
		}
	}

	private static class TinyEntry {
		
		// Value is never used, but is assigned. Probably will be used in the future
		@SuppressWarnings("unused")
		String intermediary;
		@SuppressWarnings("unused")
		String officialDescriptor;

		TinyEntry(String intermediary, String officialDescriptor) {
			this.intermediary = intermediary;
			this.officialDescriptor = officialDescriptor;
		}
	}
}
