package net.patchworkmc.patcher.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// this should be a sealed class
public class VersionRange {
	private final HashSet<MinecraftVersion> compatibleVersions = new HashSet<>();
	private static final Set<MinecraftVersion> allVersions = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MinecraftVersion.values())));

	public static VersionRange ofRange(MinecraftVersion start, MinecraftVersion end) {
		boolean collect = false;
		ArrayList<MinecraftVersion> compatibleVersions = new ArrayList<>(2);

		for (MinecraftVersion value : MinecraftVersion.values()) {
			if (!collect && value == start) {
				collect = true;
			}

			compatibleVersions.add(value);

			if (value == end) {
				break;
			}
		}

		return new VersionRange(compatibleVersions);
	}

	public static VersionRange of(MinecraftVersion... versions) {
		return new VersionRange(Arrays.asList(versions));
	}

	public static VersionRange ofAll() {
		return new VersionRange() {
			@Override
			public boolean isCompatible(MinecraftVersion version) {
				return true;
			}

			@Override
			public Set<MinecraftVersion> getCompatibleVersions() {
				return allVersions;
			}
		};
	}

	private VersionRange(Collection<MinecraftVersion> versions) {
		this.compatibleVersions.addAll(versions);
	}

	private VersionRange() {
		//
	}

	public boolean isCompatible(MinecraftVersion version) {
		return compatibleVersions.contains(version);
	}

	public Set<MinecraftVersion> getCompatibleVersions() {
		return Collections.unmodifiableSet(compatibleVersions);
	}
}
