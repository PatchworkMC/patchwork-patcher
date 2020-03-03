package com.patchworkmc.annotation;

import java.util.ArrayList;

import com.google.gson.Gson;

public class AnnotationStorage {
	private ArrayList<ClassAnnotation> classAnnotations = new ArrayList<>();
	private ArrayList<FieldAnnotation> fieldAnnotations = new ArrayList<>();
	private ArrayList<MethodAnnotation> methodAnnotations = new ArrayList<>();

	public AnnotationStorage() {
	}

	public void acceptClassAnnotation(String targetClass) {
		classAnnotations.add(new ClassAnnotation(targetClass));
	}

	public void acceptFieldAnnotation(String classIn, String fieldName) {
		fieldAnnotations.add(new FieldAnnotation(classIn, fieldName));
	}

	public void acceptMethodAnnotation(String methodSignature) {
		methodAnnotations.add(new MethodAnnotation(methodSignature));
	}

	private AnnotationStorageObject toObject() {
		return new AnnotationStorageObject(
				classAnnotations.toArray(new ClassAnnotation[0]),
				fieldAnnotations.toArray(new FieldAnnotation[0]),
				methodAnnotations.toArray(new MethodAnnotation[0])
		);
	}

	public String getJson(Gson gson) {
		AnnotationStorageObject object = toObject();
		return gson.toJson(object);
	}

	public static class ClassAnnotation {
		public String targetClass;

		public ClassAnnotation(String targetClass) {
			this.targetClass = targetClass;
		}
	}

	public static class FieldAnnotation {
		public String classIn;
		public String fieldName;

		public FieldAnnotation(String classIn, String fieldName) {
			this.classIn = classIn;
			this.fieldName = fieldName;
		}
	}

	public static class MethodAnnotation {
		public String methodSignature;

		public MethodAnnotation(String methodSignature) {
			this.methodSignature = methodSignature;
		}
	}

	public static class AnnotationStorageObject {

		public ClassAnnotation[] classAnnotations;
		public FieldAnnotation[] fieldAnnotations;
		public MethodAnnotation[] methodAnnotations;

		public AnnotationStorageObject(
				ClassAnnotation[] classAnnotations,
				FieldAnnotation[] fieldAnnotations,
				MethodAnnotation[] methodAnnotations
		) {
			this.classAnnotations = classAnnotations;
			this.fieldAnnotations = fieldAnnotations;
			this.methodAnnotations = methodAnnotations;
		}
	}
}
