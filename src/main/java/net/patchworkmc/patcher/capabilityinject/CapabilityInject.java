package net.patchworkmc.patcher.capabilityinject;

import org.objectweb.asm.Type;

public class CapabilityInject {
	private final String name;
	private final Type type;
	private final boolean isMethod;

	public CapabilityInject(String name, Type type, boolean isMethod) {
		this.name = name;
		this.type = type;
		this.isMethod = isMethod;
	}

	public String getName() {
		return this.name;
	}

	public Type getType() {
		return this.type;
	}

	public boolean isMethod() {
		return this.isMethod;
	}
}
