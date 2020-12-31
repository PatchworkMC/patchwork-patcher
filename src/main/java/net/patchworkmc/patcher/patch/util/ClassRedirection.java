package net.patchworkmc.patcher.patch.util;

import java.util.HashMap;
import java.util.Map;

public class ClassRedirection {
	public final String newName;

	public final Map<String, String> entries = new HashMap<>();

	public ClassRedirection(String newName) {
		this.newName = newName;
	}

	public final ClassRedirection with(String key, String value) {
		this.entries.put(key, value);
		return this;
	}
}