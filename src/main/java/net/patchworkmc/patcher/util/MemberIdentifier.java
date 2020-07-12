package net.patchworkmc.patcher.util;

import java.util.Objects;

public class MemberIdentifier {
	private final String name;
	private final String descriptor;

	public MemberIdentifier(String name, String descriptor) {
		this.name = name;
		this.descriptor = descriptor;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}

	@Override
	public String toString() {
		return "FieldIdentifier{name='" + name + "', descriptor='" + descriptor + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		MemberIdentifier fieldInfo = (MemberIdentifier) o;

		return Objects.equals(name, fieldInfo.name) && Objects.equals(descriptor, fieldInfo.descriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, descriptor);
	}
}
