package edu.mayo.mprc.swift.commands;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import edu.mayo.mprc.utilities.ExcludeJson;

public class AnnotationExclusionStrategy implements ExclusionStrategy {

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return f.getAnnotation(ExcludeJson.class) != null;
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		return false;
	}
}
