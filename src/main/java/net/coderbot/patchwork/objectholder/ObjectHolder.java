package net.coderbot.patchwork.objectholder;

public class ObjectHolder {
	private String field;
	private String descriptor;
	private String namespace;
	private String name;

	public ObjectHolder(String field, String descriptor, String namespace, String name) {
		this.field = field;
		this.descriptor = descriptor;
		this.namespace = namespace;
		this.name = name;
	}

	protected ObjectHolder(ObjectHolder entry) {
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
		return "ObjectHolder{" +
				"field='" + field + '\'' +
				", descriptor='" + descriptor + '\'' +
				", namespace='" + namespace + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
