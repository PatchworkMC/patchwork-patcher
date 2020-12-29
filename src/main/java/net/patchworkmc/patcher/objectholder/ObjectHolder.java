package net.patchworkmc.patcher.objectholder;

/**
 * A representation of an ObjectHolder annotation on a certain field.
 * Note that Forge allows you to make certain assumptions, like the namespace being implied by an ObjectHolder annotation on a class,
 * or a @Mod annotation implying @ObjectHolder.
 */
public final class ObjectHolder {
	private final String field;
	private final String descriptor;
	private final String namespace;
	private final String name;

	protected ObjectHolder(String field, String descriptor, String namespace, String name) {
		this.field = field;
		this.descriptor = descriptor;
		this.namespace = namespace;
		this.name = name;
	}

	public String getField() {
		return field;
	}

	public String getDescriptor() {
		return descriptor;
	}

	/**
	 * @return the namespace of this ObjectHolder. In order, this could be: the first part of the
	 * string passed (if it is an identifier), the mod id given in the @ObjectHolder annotation on the class that owns this field,
	 * or the mod id in the @Mod annotation on that class.
	 */
	public String getNamespace() {
		return namespace;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "ObjectHolder{" + "field='" + field + '\'' + ", descriptor='" + descriptor + '\'' + ", namespace='" + namespace + '\'' + ", name='" + name + '\'' + '}';
	}
}
