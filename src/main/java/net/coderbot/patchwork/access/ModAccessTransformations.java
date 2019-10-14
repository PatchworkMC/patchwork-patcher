package net.coderbot.patchwork.access;

import java.util.HashMap;
import java.util.Map;

public class ModAccessTransformations {
	private ModAccessTransformation classTransformer;
	private Map<String, ModAccessTransformation> fieldTransformers;
	private Map<String, Map<String, ModAccessTransformation>> methodTransformers;

	public ModAccessTransformations() {
		this.classTransformer = ModAccessTransformation.NONE;
		this.fieldTransformers = new HashMap<>();
		this.methodTransformers = new HashMap<>();
	}

	// TODO: Handle duplicate access transformations
	// These exceptions are way too strict, most access transformations should not conflict.

	public void setClassTransformation(ModAccessTransformation classTransformer) {
		if(!this.classTransformer.equals(ModAccessTransformation.NONE) &&
				!classTransformer.equals(this.classTransformer)) {
			throw new IllegalStateException(
					"FIXME: Conflicting class access transformations: tried to add a transform of " +
					classTransformer + " when " + this.classTransformer + " already exists");
		}

		this.classTransformer = classTransformer;
	}

	public void addFieldTransformation(String field, ModAccessTransformation transformation) {
		fieldTransformers.computeIfPresent(field, (name, existing) -> {
			if(!existing.equals(ModAccessTransformation.NONE) && !transformation.equals(existing)) {
				throw new IllegalStateException(
						"FIXME: Conflicting field access transformations: tried to add a transform of " +
						transformation + " to " + field + " when " + existing + " already exists");
			}

			return existing;
		});

		fieldTransformers.put(field, transformation);
	}

	public void addMethodTransformation(String method,
			String descriptor,
			ModAccessTransformation transformation) {
		Map<String, ModAccessTransformation> transformationSet = methodTransformers.get(method);

		if(transformationSet != null) {
			transformationSet.computeIfPresent(descriptor, (x, existing) -> {
				if(!existing.equals(ModAccessTransformation.NONE) &&
						!transformation.equals(existing)) {
					throw new IllegalStateException(
							"FIXME: Conflicting field access transformations: tried to add a transform of " +
							transformation + " to " + method + " (descriptor: " + descriptor +
							") when " + existing + " already exists");
				}

				return existing;
			});
		}

		methodTransformers.computeIfAbsent(method, n -> new HashMap<>())
				.put(descriptor, transformation);
	}

	public ModAccessTransformation getClassTransformation() {
		return classTransformer;
	}

	public ModAccessTransformation getFieldTransformation(String field) {
		return fieldTransformers.getOrDefault(field, ModAccessTransformation.NONE);
	}

	public ModAccessTransformation getMethodTransformation(String method, String descriptor) {
		Map<String, ModAccessTransformation> transformationSet = methodTransformers.get(method);

		if(transformationSet != null) {
			return transformationSet.getOrDefault(descriptor, ModAccessTransformation.NONE);
		}

		return ModAccessTransformation.NONE;
	}
}
