package com.patchworkmc.jar;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import io.github.fukkitmc.gloom.definitions.ClassDefinition;
import io.github.fukkitmc.gloom.definitions.GloomDefinitions;

import com.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.manifest.mod.ModManifestEntry;

public class ForgeModJar {
	private Path jarPath;
	private ModManifest manifest;
	private GloomDefinitions gloomDefs;
	private HashSet<String> depedencies;

	public ForgeModJar(Path jarPath, ModManifest manifest, GloomDefinitions gloomDefs) {
		this.jarPath = jarPath;
		this.manifest = manifest;
		this.gloomDefs = gloomDefs;
		this.depedencies = new HashSet<>();
		// Store all of our dependencies into one Set
		this.manifest.getDependencyMapping().forEach((modId, list) ->
					list.forEach((modManifestDependency ->
							this.depedencies.add(modManifestDependency.getModId()))));
	}

	public void addDependencyJars(List<ForgeModJar> modJars) {
		for (ForgeModJar proposedDependencyJar : modJars) {
			if (proposedDependencyJar.equals(this)) {
				continue;
			}

			boolean depends = false;

			for (ModManifestEntry modManifestEntry : proposedDependencyJar.getManifest().getMods()) {
				if (this.depedencies.contains(modManifestEntry.getModId())) {
					depends = true;
					break;
				}
			}

			if (depends) {
				for (ClassDefinition definition : proposedDependencyJar.getGloomDefinitions().getDefinitions()) {
					this.gloomDefs = this.gloomDefs.merge(definition);
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

	public GloomDefinitions getGloomDefinitions() {
		return gloomDefs;
	}
}
