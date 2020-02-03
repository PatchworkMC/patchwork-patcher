package com.patchworkmc.jar;

import java.nio.file.Path;
import java.util.List;

import com.patchworkmc.manifest.accesstransformer.AccessTransformerList;
import com.patchworkmc.manifest.converter.com.patchworkmc.manifest.AccessTransformerListMerger;
import com.patchworkmc.manifest.mod.ModManifest;
import com.patchworkmc.manifest.mod.ModManifestEntry;

public class ForgeModJar {
	private Path jarPath;
	private ModManifest manifest;
	private AccessTransformerList accessTransformers;

	public ForgeModJar(Path jarPath, ModManifest manifest, AccessTransformerList list) {
		this.jarPath = jarPath;
		this.manifest = manifest;
		this.accessTransformers = list;
	}

	public void addDependencyJars(List<ForgeModJar> modJars) {
		for (ForgeModJar mod : modJars) {
			if (mod.equals(this)) {
				continue;
			}

			boolean depends = false;

			for (ModManifestEntry modManifestEntry : getManifest().getMods()) {
				if (manifest.getDependencyMapping().containsKey(modManifestEntry.getModId())) {
					depends = true;
					break;
				}
			}

			if (depends) {
				this.accessTransformers = AccessTransformerListMerger.createMergedList(this.accessTransformers, mod.getAccessTransformers());
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
