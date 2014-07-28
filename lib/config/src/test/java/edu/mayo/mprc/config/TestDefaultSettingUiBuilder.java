package edu.mayo.mprc.config;

import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestDefaultSettingUiBuilder {
	@Test
	public void shouldGetDefaultSettings() {
		DefaultSettingUiBuilder builder = new DefaultSettingUiBuilder(
				ImmutableMap.of("hi", "hello"), new DependencyResolver(null)
		);
		builder.property("test", "Test", "...")
				.defaultValue("default");

		builder.propertyArray("prefix", "Sub array", "...")
				.property("prop1", "Property 1", "...")
				.defaultValue("default 1")
				.propertyArrayEnd();

		Assert.assertEquals(builder.getValue("test"), "default");
		Assert.assertEquals(builder.getValue("hi"), "hello");
		Assert.assertEquals(builder.getValue("prefix.23.prop1"), "default 1");
	}

}
