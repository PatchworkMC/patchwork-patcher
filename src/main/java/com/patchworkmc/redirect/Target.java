package com.patchworkmc.redirect;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Target target = (Target) o;
		return owner.equals(target.owner) && name.equals(target.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, name);
	}

	@Override
	public String toString() {
		return owner + "." + name;
	}
}
