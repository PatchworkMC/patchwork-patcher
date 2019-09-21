package net.coderbot.patchwork.manifest.forge;

import java.util.Map;

/**
 * A dependency entry contained within a mods.toml file.
 */
public class ModManifestDependency {
	private String modId;
	private boolean mandatory;
	private String versionRange;
	private String ordering;
	private String side;

	private ModManifestDependency() {}

	/**
	 * Parses a TOML object into a ModManifestDependency entry.
	 *
	 * @param data A Map containing the parsed form of the TOML object
	 * @return A ModManifestDependency entry parsed from the specified TOML object
	 * @throws ManifestParseException if one of the keys has a wrong data type or a required key is missing
	 */
	public static ModManifestDependency parse(Map<String, Object> data) throws ManifestParseException {
		ModManifestDependency dependency = new ModManifestDependency();

		try {

			// Check these "required" bounds
			dependency.modId = ManifestParseHelper.getString(data, "modId", true);
			dependency.mandatory = data.get("mandatory") == Boolean.TRUE;
			dependency.versionRange = ManifestParseHelper.getString(data, "versionRange", true);
			dependency.ordering = ManifestParseHelper.getString(data, "ordering", true);
			dependency.side = ManifestParseHelper.getString(data, "side", true);

		} catch(IllegalArgumentException e) {
			throw new ManifestParseException("Failed to parse mod manifest dependency entry", e);
		}

		return dependency;
	}

	public String getModId() {
		return modId;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public String getVersionRange() {
		return versionRange;
	}

	public String getOrdering() {
		return ordering;
	}

	public String getSide() {
		return side;
	}

	@Override
	public String toString() {
		return "ModManifestDependency{" +
				"modId='" + modId + '\'' +
				", mandatory=" + mandatory +
				", versionRange='" + versionRange + '\'' +
				", ordering='" + ordering + '\'' +
				", side='" + side + '\'' +
				'}';
	}
}
