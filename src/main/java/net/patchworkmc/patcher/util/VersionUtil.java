package net.patchworkmc.patcher.util;

public final class VersionUtil {
	private VersionUtil() { }

	public static String getMinecraftVersion() {
		return "1.14.4";
	}

	public static String getForgeVersion() {
		return getMinecraftVersion() + "28-2.23";
	}

	public static String getMcpConfigVersion() {
		return getMinecraftVersion() + "-20191210.153145";
	}
}
