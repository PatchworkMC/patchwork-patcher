package com.patchworkmc.jar;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.patchworkmc.manifest.accesstransformer.AccessTransformerList;
import com.patchworkmc.manifest.AccessTransformerListMerger;
import com.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.manifest.mod.ModManifestEntry;

public class ForgeModJar {
	private Path jarPath;
	private ModManifest manifest;
	private AccessTransformerList accessTransformers;
	private Set<String> dependencies = new HashSet<>();

	public ForgeModJar(Path jarPath, ModManifest manifest, AccessTransformerList list) {
		this.jarPath = jarPath;
		this.manifest = manifest;
		this.accessTransformers = list;
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
				this.accessTransformers = AccessTransformerListMerger.createMergedList(this.accessTransformers, proposedDependencyJar.getAccessTransformers());
			}
		}
	}

	public Path getJarPath() {
		return jarPath;
	}

	public ModManifest getManifest() {
		return manifest;
	}

	public AccessTransformerList getAccessTransformers() {
		return accessTransformers;
	}
}
