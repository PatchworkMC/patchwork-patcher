package com.patchworkmc.jar;

import java.nio.file.Path;
import java.util.List;

import io.github.fukkitmc.gloom.definitions.ClassDefinition;
import io.github.fukkitmc.gloom.definitions.GloomDefinitions;

import com.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.manifest.mod.ModManifestEntry;

public class ForgeModJar {
	private Path jarPath;
	private ModManifest manifest;
	private GloomDefinitions definitions;

	public ForgeModJar(Path jarPath, ModManifest manifest, GloomDefinitions definitions) {
		this.jarPath = jarPath;
		this.manifest = manifest;
		this.definitions = definitions;
	}

	public void addDependencyJars(List<ForgeModJar> modJars) {
		for (ForgeModJar proposedDependencyJar : modJars) {
			if (proposedDependencyJar.equals(this)) {
				continue;
			}

			boolean depends = false;

			for (ModManifestEntry modManifestEntry : proposedDependencyJar.getManifest().getMods()) {
				if (this.manifest.getDependencyMapping().containsKey(modManifestEntry.getModId())) {
					depends = true;
					break;
				}
			}

			if (depends) {
				for (ClassDefinition definition : proposedDependencyJar.getDefinitions().getDefinitions()) {
					this.definitions.merge(definition);
				}
			}
		}
	}

	public Path getJarPath() {
		return jarPath;
	}

	public ModManifest getManifest() {
		return manifest;
	}

	public GloomDefinitions getDefinitions() {
		return definitions;
	}
}
