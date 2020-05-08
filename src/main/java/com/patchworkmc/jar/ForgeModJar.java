package com.patchworkmc.jar;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import io.github.fukkitmc.gloom.definitions.ClassDefinition;
import io.github.fukkitmc.gloom.definitions.GloomDefinitions;

import net.patchworkmc.manifest.mod.ModManifest;
import net.patchworkmc.manifest.mod.ModManifestEntry;

public class ForgeModJar {
	private Path jarPath;
	private ModManifest manifest;

	public ForgeModJar(Path jarPath, ModManifest manifest) {
		this.jarPath = jarPath;
		this.manifest = manifest;
	}

	public Path getJarPath() {
		return jarPath;
	}

	public ModManifest getManifest() {
		return manifest;
	}
}
