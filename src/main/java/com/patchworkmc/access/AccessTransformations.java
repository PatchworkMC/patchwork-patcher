package com.patchworkmc.access;

import java.util.HashMap;
import java.util.Map;

public class AccessTransformations {
	private AccessTransformation classTransformer;
	private Map<String, AccessTransformation> fieldTransformers;
	private Map<String, Map<String, AccessTransformation>> methodTransformers;

	public AccessTransformations() {
		this.classTransformer = AccessTransformation.NONE;
		this.fieldTransformers = new HashMap<>();
		this.methodTransformers = new HashMap<>();
	}

	// TODO: Handle duplicate access transformations
	// These exceptions are way too strict, most access transformations should not conflict.

	public void addFieldTransformation(String field, AccessTransformation transformation) {
		fieldTransformers.computeIfPresent(field, (name, existing) -> {
			if (!existing.equals(AccessTransformation.NONE) && !transformation.equals(existing)) {
				throw new IllegalStateException("FIXME: Conflicting field access transformations: tried to add a transform of " + transformation + " to " + field + " when " + existing + " already exists");
			}

			return existing;
		});

		fieldTransformers.put(field, transformation);
	}

	public void addMethodTransformation(String method, String descriptor, AccessTransformation transformation) {
		Map<String, AccessTransformation> transformationSet = methodTransformers.get(method);

		if (transformationSet != null) {
			transformationSet.computeIfPresent(descriptor, (x, existing) -> {
				if (!existing.equals(AccessTransformation.NONE) && !transformation.equals(existing)) {
					throw new IllegalStateException("FIXME: Conflicting field access transformations: tried to add a transform of " + transformation + " to " + method + " (descriptor: " + descriptor + ") when " + existing + " already exists");
				}

				return existing;
			});
		}

		methodTransformers.computeIfAbsent(method, n -> new HashMap<>()).put(descriptor, transformation);
	}

	public AccessTransformation getClassTransformation() {
		return classTransformer;
	}

	public void setClassTransformation(AccessTransformation classTransformer) {
		if (!this.classTransformer.equals(AccessTransformation.NONE) && !classTransformer.equals(this.classTransformer)) {
			throw new IllegalStateException("FIXME: Conflicting class access transformations: tried to add a transform of " + classTransformer + " when " + this.classTransformer + " already exists");
		}

		this.classTransformer = classTransformer;
	}

	public AccessTransformation getFieldTransformation(String field) {
		return fieldTransformers.getOrDefault(field, AccessTransformation.NONE);
	}

	public AccessTransformation getMethodTransformation(String method, String descriptor) {
		Map<String, AccessTransformation> transformationSet = methodTransformers.get(method);

		if (transformationSet != null) {
			return transformationSet.getOrDefault(descriptor, AccessTransformation.NONE);
		}

		return AccessTransformation.NONE;
	}
}
