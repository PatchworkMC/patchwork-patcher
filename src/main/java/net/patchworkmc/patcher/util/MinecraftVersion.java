package net.patchworkmc.patcher.util;

public enum MinecraftVersion {
	V1_14_4("1.14.4");

	private final String version;

	MinecraftVersion(String versionIn) {
		this.version = versionIn;
	}

	public String getVersion() {
		return version;
	}
}
