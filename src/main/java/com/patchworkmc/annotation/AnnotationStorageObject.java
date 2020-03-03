package com.patchworkmc.annotation;

import java.lang.annotation.ElementType;

public class AnnotationStorageObject {
	public static class Entry {
		public ElementType targetType;
		public String target;

		public Entry(ElementType targetType, String target) {
			this.targetType = targetType;
			this.target = target;
		}
	}

	public Entry[] entries;

	public AnnotationStorageObject(Entry[] entries) {
		this.entries = entries;
	}
}
