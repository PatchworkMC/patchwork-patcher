package com.patchworkmc.mapping;

import net.patchworkmc.manifest.accesstransformer.v2.exception.MissingMappingException;

public class MappingAssertions {
	private MappingAssertions() {
		// NO-OP
	}

	public static void assertClassExists(Object object, String className) throws MissingMappingException {
		if (object == null) {
			throw new MissingMappingException("Unable to get mappings for class " + className);
		}
	}

	public static void assertFieldExists(Object object, String className, String fieldName) throws MissingMappingException {
		if (object == null) {
			throw new MissingMappingException("Unable to get mappings for field " + fieldName + " in class " + className);
		}
	}

	public static void assertMethodExists(Object object, String className, String methodName) throws MissingMappingException {
		if (object == null) {
			throw new MissingMappingException("Unable to get mappings for method " + methodName + " in class " + className);
		}
	}
}
