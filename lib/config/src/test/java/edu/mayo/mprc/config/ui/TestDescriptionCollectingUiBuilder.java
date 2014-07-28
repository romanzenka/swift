package edu.mayo.mprc.config.ui;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestDescriptionCollectingUiBuilder {
	private DescriptionCollectingUiBuilder builder;

	@BeforeClass
	public void setup() {
		builder = new DescriptionCollectingUiBuilder();
	}

	@Test
	public void shouldProduceSimpleDescriptions() {
		builder.property("hello", "Hello property", "Blah blah blah");
		builder.property("world", "World property", "Yak yak yak");

		Assert.assertEquals(builder.getValue("hello"), "Hello property");
		Assert.assertEquals(builder.getValue("world"), "World property");
		Assert.assertEquals(builder.getValue("test"), null);
	}

	@Test
	public void shouldProduceComplexDescriptions() {
		builder.property("simple", "Simple property", "...");
		builder.propertyArray("prefix", "Complex property", "...")
				.property("sub1", "Subproperty 1", "...")
				.property("sub2", "Subproperty 2", "...")
				.propertyArrayEnd();

		Assert.assertEquals(builder.getValue("simple"), "Simple property");
		Assert.assertEquals(builder.getValue("prefix.1.sub1"), "Subproperty 1");
		Assert.assertEquals(builder.getValue("prefix.3.sub2"), "Subproperty 2");
	}


}
