package com.patchworkmc.mapping;

import net.patchworkmc.manifest.accesstransformer.v2.exception.FatalMissingMappingException;
import net.patchworkmc.manifest.accesstransformer.v2.exception.MissingMappingException;

public class MappingAssertions {
	private MappingAssertions() {
		// NO-OP
	}
	// TODO remove and use proper exception handling in AccessTransformerConverter
	public static void fatallyAssertClassExists(Object object, String className) {
		try {
			assertClassExists(object, className);
		} catch (MissingMappingException ex) {
			throw new FatalMissingMappingException(ex);
		}
	}

	public static void fatallyAssertFieldExists(Object object, String className, String fieldName) {
		try {
			assertFieldExists(object, className, fieldName);
		} catch (MissingMappingException ex) {
			throw new FatalMissingMappingException(ex);
		}
	}

	public static void fatallyAssertMethodExists(Object object, String className, String methodName) {
		try {
			assertMethodExists(object, className, methodName);
		} catch (MissingMappingException ex) {
			throw new FatalMissingMappingException(ex);
		}
	}

	// TODO return the object if it exists via generics(?)
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
