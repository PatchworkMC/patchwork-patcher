package com.patchworkmc.redirect;

public class Target {
	private String owner;
	private String name;

	public Target(String owner, String name) {
		this.owner = owner;
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return owner + "." + name;
	}
}
