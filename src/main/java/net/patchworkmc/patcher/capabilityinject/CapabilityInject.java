package net.patchworkmc.patcher.capabilityinject;

import org.objectweb.asm.Type;

public class CapabilityInject {
	private final String name;
	private final Type type;
	private final boolean isMethod;
	private String desc;

	public CapabilityInject(String name, Type type, String desc, boolean isMethod) {
		this.name = name;
		this.type = type;
		this.isMethod = isMethod;
		this.desc = desc;
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

	public String getDesc() {
		return this.desc;
	}
}
