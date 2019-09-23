package net.coderbot.patchwork.manifest.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ModManifestEntry {
	private String modId;
	private String version;
	private String displayName;
	private String description;
	private String namespace;
	private String logoFile;
	private String updateJsonUrl;
	private String credits;
	private String authors;
	private String displayUrl;
	private Map<String, Object> properties;

	private ModManifestEntry() {}

	/**
	 * Parses a TOML object into a ModManifestEntry.
	 *
	 * @param data A Map containing the parsed form of the TOML object
	 * @return A ModManifestEntry entry parsed from the specified TOML object
	 * @throws ManifestParseException if one of the keys has a wrong data type or a required key is missing
	 */
	public static ModManifestEntry parse(Map<String, Object> data) throws ManifestParseException {
		ModManifestEntry entry = new ModManifestEntry();

		try {

			entry.modId = ManifestParseHelper.getString(data, "modId", true);
			entry.version = ManifestParseHelper.getString(data, "version", false);
			entry.displayName = ManifestParseHelper.getString(data, "displayName", false);
			entry.description = ManifestParseHelper.getString(data, "description", true);
			entry.namespace = ManifestParseHelper.getString(data, "namespace", false);
			entry.logoFile = ManifestParseHelper.getString(data, "logoFile", false);
			entry.updateJsonUrl = ManifestParseHelper.getString(data, "updateJSONURL", false);
			entry.credits = ManifestParseHelper.getString(data, "credits", false);
			entry.authors = ManifestParseHelper.getString(data, "authors", false);
			entry.displayUrl = ManifestParseHelper.getString(data, "displayURL", false);
			entry.properties = ManifestParseHelper.getMap(data, "properties", false);

		} catch(Exception e) {
			throw new ManifestParseException("Failed to parse mod manifest mod entry", e);
		}

		if(entry.namespace == null) {
			entry.namespace = entry.modId;
		}

		// TODO: Forge processes the version here???

		if(entry.version == null) {
			entry.version = "1";
		}

		if(entry.displayName == null) {
			entry.displayName = entry.modId;
		}

		if(entry.properties == null) {
			entry.properties = new HashMap<>();
		}

		return entry;
	}

	public String getModId() {
		return modId;
	}

	public String getVersion() {
		return version;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}

	public String getNamespace() {
		return namespace;
	}

	public Optional<String> getLogoFile() {
		return Optional.ofNullable(logoFile);
	}

	public Optional<String> getUpdateJsonUrl() {
		return Optional.ofNullable(updateJsonUrl);
	}

	public Optional<String> getCredits() {
		return Optional.ofNullable(credits);
	}

	public Optional<String> getAuthors() {
		return Optional.ofNullable(authors);
	}

	public Optional<String> getDisplayUrl() {
		return Optional.ofNullable(displayUrl);
	}

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "ModManifestEntry{" +
				"modId='" + modId + '\'' +
				", version='" + version + '\'' +
				", displayName='" + displayName + '\'' +
				", description='" + description + '\'' +
				", namespace='" + namespace + '\'' +
				", logoFile='" + logoFile + '\'' +
				", updateJsonUrl='" + updateJsonUrl + '\'' +
				", credits='" + credits + '\'' +
				", authors='" + authors + '\'' +
				", displayUrl='" + displayUrl + '\'' +
				", properties=" + properties +
				'}';
	}
}
