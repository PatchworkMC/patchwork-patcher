package net.patchworkmc.patcher;

import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.patchworkmc.manifest.accesstransformer.v2.ForgeAccessTransformer;
import net.patchworkmc.manifest.mod.ModManifest;
import net.patchworkmc.patcher.annotation.AnnotationStorage;

public class ForgeModJar {
	private final Path input, output;
	private final ModManifest manifest;
	private final ForgeAccessTransformer at;
	private final AnnotationStorage annotationStorage;
	private final JsonObject entrypoints;
	protected boolean processed;

	public ForgeModJar(Path input, Path output, ModManifest manifest) {
		this(input, output, manifest, null);
	}

	public ForgeModJar(Path input, Path output, ModManifest manifest, ForgeAccessTransformer at) {
		this.input = input;
		this.output = output;
		this.manifest = manifest;
		this.at = at;
		this.annotationStorage = new AnnotationStorage();
		this.entrypoints = new JsonObject();
	}

	public Path getInputPath() {
		return input;
	}

	public Path getOutputPath() {
		return output;
	}

	public ModManifest getManifest() {
		return manifest;
	}

	public ForgeAccessTransformer getAccessTransformer() {
		return at;
	}

	public AnnotationStorage getAnnotationStorage() {
		return annotationStorage;
	}

	public void addEntrypoint(String key, String value) {
		value = value.replace('/', '.');
		JsonArray entrypointList = this.entrypoints.getAsJsonArray(key);
		if (entrypointList == null) {
			JsonArray arr = new JsonArray();
			this.entrypoints.add(key, arr);
			entrypointList = arr;
		}

		entrypointList.add(value);
	}

	public JsonObject getEntrypoints() {
		return entrypoints.deepCopy();
	}

	public boolean isProcessed() {
		return processed;
	}
}
