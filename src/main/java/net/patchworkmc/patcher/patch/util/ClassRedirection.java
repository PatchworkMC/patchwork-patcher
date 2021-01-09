package net.patchworkmc.patcher.patch.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.patchworkmc.patcher.util.IllegalArgumentError;

public final class ClassRedirection {
	public final String newOwner;

	public final Map<String, String> mapEntries = new HashMap<>();
	public final Set<String> setEntries = new HashSet<>();
	public ClassRedirection(String newOwner) {
		this.newOwner = newOwner;
	}

	public ClassRedirection with(String key, String value) {
		this.mapEntries.put(key, value);
		return this;
	}

	public ClassRedirection with(String key) {
		this.setEntries.add(key);
		return this;
	}

	public boolean contains(String key) {
		return this.mapEntries.containsKey(key) || this.setEntries.contains(key);
	}

	public void assertNoSetEntries() {
		if (!setEntries.isEmpty()) {
			throw new IllegalArgumentError(String.format("Expected no set entries, found %s", this.setEntries));
		}
	}
}
