package net.patchworkmc.patcher.util;
public enum MinecraftVersion {
	V1_14_4("1.14.4"),
	V1_15_2("1.15.2"),
	V1_16_3("1.16.3");

	private final String version;

	MinecraftVersion(String versionIn) {
		this.version = versionIn;
	}

	public String getVersion() {
		return version;
	}
}
