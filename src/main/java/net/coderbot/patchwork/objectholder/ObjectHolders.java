package net.coderbot.patchwork.objectholder;

import java.util.*;

public class ObjectHolders {
	private String defaultModId;
	private Map<String, String> objectHolders;

	public ObjectHolders() {
		this.defaultModId = null;
		this.objectHolders = new HashMap<>();
	}

	public void setDefaultModId(String modId) {
		this.defaultModId = modId;
	}

	public void addObjectHolder(String field, String identifier) {
		objectHolders.put(field, identifier);
	}

	public String getDefaultModId() {
		return defaultModId;
	}

	public Map<String, String> getObjectHolders() {
		return Collections.unmodifiableMap(objectHolders);
	}

	public List<Entry> process(Map<String, String> fieldDescriptors) {
		List<Entry> list = new ArrayList<>();

		for(Map.Entry<String, String> fieldEntry: fieldDescriptors.entrySet()) {
			String field = fieldEntry.getKey();
			String descriptor = fieldEntry.getValue();

			Entry entry = new Entry();

			String identifier = objectHolders.get(field);

			if(identifier != null) {
				if(identifier.contains(":")) {
					String[] parts = identifier.split(":");

					entry.namespace = parts[0];
					entry.name = parts[1];
				} else if(defaultModId != null) {
					entry.namespace = defaultModId;
					entry.name = identifier;
				} else {
					throw new IllegalArgumentException("Missing class-level @ObjectHolder declaration providing a mod id");
				}
			}  else if(defaultModId != null) {
				entry.namespace = defaultModId;
				entry.name = field.toLowerCase();
			} else {
				throw new IllegalArgumentException("Missing class-level @ObjectHolder declaration providing a mod id");
			}

			entry.field = field;
			entry.descriptor = descriptor;

			list.add(entry);
		}

		return list;
	}

	public static class Entry {
		private String field;
		private String descriptor;
		private String namespace;
		private String name;

		private Entry() {}

		protected Entry(Entry entry) {
			this.field = entry.field;
			this.descriptor = entry.descriptor;
			this.namespace = entry.namespace;
			this.name = entry.name;
		}

		public String getField() {
			return field;
		}

		public String getDescriptor() {
			return descriptor;
		}

		public String getNamespace() {
			return namespace;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "Entry{" +
					"field='" + field + '\'' +
					", descriptor='" + descriptor + '\'' +
					", namespace='" + namespace + '\'' +
					", name='" + name + '\'' +
					'}';
		}
	}
}
