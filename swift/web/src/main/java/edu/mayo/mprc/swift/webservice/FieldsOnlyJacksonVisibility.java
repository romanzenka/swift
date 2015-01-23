package edu.mayo.mprc.swift.webservice;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.introspect.VisibilityChecker.Std;
import org.springframework.stereotype.Component;

/**
 * We configure Jackson to only look for fields, just like XStream.
 * <p/>
 * We return a visibility checker with getters/setters disabled and use
 * it to create a custom Jackson serializer.
 *
 * @author Roman Zenka
 */
@Component("fieldsOnlyJacksonChecker")
public final class FieldsOnlyJacksonVisibility {

	public VisibilityChecker checker() {
		return Std.defaultInstance().withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withIsGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE);
	}
}
