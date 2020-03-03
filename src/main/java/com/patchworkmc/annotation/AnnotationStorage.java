package com.patchworkmc.annotation;

import java.lang.annotation.ElementType;
import java.util.ArrayList;

import com.google.gson.Gson;

public class AnnotationStorage {
	private ArrayList<AnnotationStorageObject.Entry> entries = new ArrayList<>();

	public AnnotationStorage() {
	}

	public void accept(ElementType elementType, String target) {
		entries.add(new AnnotationStorageObject.Entry(elementType, target));
	}

	private AnnotationStorageObject toObject() {
		return new AnnotationStorageObject(entries.toArray(new AnnotationStorageObject.Entry[0]));
	}

	public String getJson(Gson gson) {
		AnnotationStorageObject object = toObject();
		return gson.toJson(object);
	}
}
