package net.patchworkmc.patcher.util;

public enum MinecraftVersion {
	V1_16_5("1.16.5");

	private final String version;

	MinecraftVersion(String versionIn) {
		this.version = versionIn;
	}

	public String getVersion() {
		return version;
	}
}
