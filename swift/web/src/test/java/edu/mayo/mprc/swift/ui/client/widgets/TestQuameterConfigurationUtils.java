package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;

/**
 * @author Roman Zenka
 */
public final class TestQuameterConfigurationUtils {

	public static final String CATEGORIES = "animal,-cat,--siamese*,-dog,--chihuahua";

	@Test
	public void shouldParseCategoryString() {
		final LinkedHashMap<String, String> map = QuameterConfigurationUtils.parseCategories(CATEGORIES);
		Assert.assertEquals(map,
				new ImmutableMap.Builder<String, String>()
						.put("animal", "animal")
						.put(" - cat", "cat")
						.put(" -  - siamese", "siamese")
						.put(" - dog", "dog")
						.put(" -  - chihuahua", "chihuahua")
						.build());
	}

	@Test
	public void shouldGetDefaultCategory() {
		Assert.assertEquals(
				QuameterConfigurationUtils.getDefaultCategory(CATEGORIES),
				"siamese");
		Assert.assertEquals(
				QuameterConfigurationUtils.getDefaultCategory(" hello , world*"),
				"world");
		Assert.assertEquals(
				QuameterConfigurationUtils.getDefaultCategory(" hello , world"),
				"hello");
	}
}
