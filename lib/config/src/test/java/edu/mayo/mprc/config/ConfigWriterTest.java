package edu.mayo.mprc.config;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class ConfigWriterTest {
	private DependencyResolver resolver;
	private TestResource resource;

	@BeforeClass
	public void init() {
		resolver = new DependencyResolver(new ResourceFactory<ResourceConfig, Object>() {
			@Override
			public Object create(ResourceConfig config, DependencyResolver dependencies) {
				return null;
			}

			@Override
			public Object createSingleton(ResourceConfig config, DependencyResolver dependencies) {
				return null;
			}
		});


		resource = new TestResource();
		resolver.addConfig("ref1", resource.getRef1());
		resolver.addConfig("ref2", resource.getRef2());
	}

	@Test
	public void shouldSaveToMap() {

		final Map<String, String> map = MapConfigWriter.save(resource, resolver);

		Assert.assertEquals(map.size(), 6);
		Assert.assertEquals(map.get("integer"), "123");
		Assert.assertEquals(map.get("resource"), "ref1");
		Assert.assertEquals(map.get("resources"), "ref1, ref2");
	}

	@Test
	public void shouldSaveKey() {
		Assert.assertEquals(KeyExtractingWriter.get(resource, "integer"), "123");
		Assert.assertEquals(KeyExtractingWriter.get(resource, "boolean"), "true");
		Assert.assertEquals(KeyExtractingWriter.get(resource, "key"), "value");
		Assert.assertEquals(KeyExtractingWriter.get(resource, "resource"), "");
		Assert.assertEquals(KeyExtractingWriter.get(resource, "resources"), "");
	}


}
