package net.patchworkmc.annotation;

import java.lang.annotation.ElementType;
import java.util.ArrayList;

import com.google.gson.Gson;

public class AnnotationStorage {
	public static final String relativePath = "/annotations.json";

	private static class Entry {
		public String annotationType;
		public ElementType targetType;
		public String targetInClass;
		public String target;

		Entry(String annotationType, ElementType targetType, String targetInClass, String target) {
			this.annotationType = annotationType;
			this.targetType = targetType;
			this.targetInClass = targetInClass;
			this.target = target;
		}
	}

	private ArrayList<Entry> entries = new ArrayList<>();

	public AnnotationStorage() {
	}

	public void acceptClassAnnotation(String annotation, String targetClass) {
		entries.add(new Entry(annotation, ElementType.TYPE, targetClass, targetClass));
	}

	public void acceptFieldAnnotation(String annotation, String clazz, String field) {
		entries.add(new Entry(annotation, ElementType.FIELD, clazz, field));
	}

	public void acceptMethodAnnotation(String annotation, String clazz, String method) {
		entries.add(new Entry(annotation, ElementType.METHOD, clazz, method));
	}

	public String toJson(Gson gson) {
		return gson.toJson(this);
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}
}
