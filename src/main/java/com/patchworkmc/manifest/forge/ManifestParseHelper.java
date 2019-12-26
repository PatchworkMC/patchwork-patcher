package com.patchworkmc.manifest.forge;

import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.AbstractConfig;

public class ManifestParseHelper {
	private static Object getEntry(Map<String, Object> data, String key, boolean required)
		throws ManifestParseException {
		Object entry = data.get(key);

		if (entry == null && required) {
			throw new ManifestParseException("Missing entry \"" + key + "\" in data!");
		}

		return entry;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> toMap(Object object, String thing) throws ManifestParseException {
		if (object instanceof AbstractConfig) {
			return ((AbstractConfig) object).valueMap();
		} else if (object instanceof Map) {
			return (Map<String, Object>) object;
		} else {
			throw ManifestParseHelper.typeError(thing, object, "Map or AbstractConfig");
		}
	}

	public static String getString(Map<String, Object> data, String key, boolean required)
		throws ManifestParseException {
		Object entry = getEntry(data, key, required);

		if (entry == null) {
			return null;
		} else if (entry instanceof String) {
			return (String) entry;
		} else {
			throw typeError("Entry \"" + key + '"', entry, "String");
		}
	}

	// TOML can only contain maps of type Map<String, Object)
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getMap(Map<String, Object> data, String key, boolean required)
		throws ManifestParseException {
		Object entry = getEntry(data, key, required);

		if (entry == null) {
			return null;
		}

		return toMap(entry, "Entry \"" + key + '"');
	}

	public static List getList(Map<String, Object> data, String key, boolean required) throws ManifestParseException {
		Object entry = getEntry(data, key, required);

		if (entry == null) {
			return null;
		} else if (entry instanceof List) {
			return (List) entry;
		} else {
			throw typeError("Entry \"" + key + '"', entry, "List");
		}
	}

	public static ManifestParseException typeError(String thing, Object value, String expected) {
		return new ManifestParseException(thing + " was an instance of " + value.getClass().getSimpleName() + " but an instance of " + expected + " was expected!");
	}
}
