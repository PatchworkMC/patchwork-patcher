package net.coderbot.patchwork.access;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AccessTransformations {
	private Map<String, AccessTransformation> fieldTransformers;
	private Map<String, Map<String, AccessTransformation>> methodTransformers;

	public AccessTransformations() {
		this.fieldTransformers = new HashMap<>();
		this.methodTransformers = new HashMap<>();
	}

	// TODO: Check for duplicates

	public void addFieldTransformation(String field, AccessTransformation transformation) {
		fieldTransformers.put(field, transformation);
	}

	public void addMethodTransformation(String method,
			String descriptor,
			AccessTransformation transformation) {
		methodTransformers.computeIfAbsent(method, n -> new HashMap<>())
				.put(descriptor, transformation);
	}

	public Optional<AccessTransformation> getFieldTransformation(String field) {
		return Optional.ofNullable(fieldTransformers.get(field));
	}

	public Optional<AccessTransformation> getMethodTransformation(String method,
			String descriptor) {
		Map<String, AccessTransformation> transformationSet = methodTransformers.get(method);

		if(transformationSet != null) {
			return Optional.ofNullable(transformationSet.get(descriptor));
		}

		return Optional.empty();
	}
}
