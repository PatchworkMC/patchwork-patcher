package com.patchworkmc.mapping;

import java.util.HashMap;
import java.util.Map;

public class MemberMap {
	Map<String, String> members;

	public MemberMap() {
		this.members = new HashMap<>();
	}

	public void put(String owner, String name, String value) {
		members.put(id(owner, name), value);
	}

	public String get(String owner, String name) {
		return members.get(id(owner, name));
	}

	private String id(String owner, String name) {
		if (owner.contains(";")) {
			throw new IllegalArgumentException("owner may not contain a ; character: " + owner);
		}

		if (name.contains(";")) {
			throw new IllegalArgumentException("name may not contain a ; character: " + name);
		}

		return owner + ";;" + name;
	}
}
