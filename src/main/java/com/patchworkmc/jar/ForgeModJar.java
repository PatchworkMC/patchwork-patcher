package com.patchworkmc.jar;

import java.nio.file.Path;

import net.patchworkmc.manifest.accesstransformer.v2.ForgeAccessTransformer;
import net.patchworkmc.manifest.mod.ModManifest;

public class ForgeModJar {
	private final Path jarPath;
	private final ModManifest manifest;
	private final ForgeAccessTransformer at;

	public ForgeModJar(Path jarPath, ModManifest manifest) {
		this(jarPath, manifest, null);
	}

	public ForgeModJar(Path jarPath, ModManifest manifest, ForgeAccessTransformer at) {
		this.jarPath = jarPath;
		this.manifest = manifest;
		this.at = at;
	}

	public Path getJarPath() {
		return jarPath;
	}

	public ModManifest getManifest() {
		return manifest;
	}

	public ForgeAccessTransformer getAccessTransformer() {
		return at;
	}
}
