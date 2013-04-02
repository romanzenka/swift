package edu.mayo.mprc.config;

import org.testng.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Zenka
 */
class TestResource implements ResourceConfig {
	private TestResource2 ref1;
	private TestResource2 ref2;

	TestResource() {
		ref1 = new TestResource2();
		ref2 = new TestResource2();
	}

	public TestResource2 getRef1() {
		return ref1;
	}

	public TestResource2 getRef2() {
		return ref2;
	}

	@Override
	public void save(ConfigWriter writer) {
		writer.comment("Test resource");
		writer.put("boolean", true);
		writer.put("integer", 123, "Integer");
		writer.put("key", "value");
		writer.put("key2", "value2", "Comment");
		writer.put("resource", ref1);
		writer.put("resources", Arrays.asList(ref1, ref2));
	}

	@Override
	public void load(ConfigReader reader) {
		Assert.assertEquals(reader.getBoolean("boolean"), true);
		Assert.assertEquals(reader.getInteger("integer"), 123);
		Assert.assertEquals(reader.get("key"), "value");
		Assert.assertEquals(reader.get("key2"), "value2");
		final ResourceConfig resource = reader.getObject("resource");
		Assert.assertEquals(resource.getClass(), TestResource2.class);
		final List<? extends ResourceConfig> resources = reader.getResourceList("resources");
		Assert.assertEquals(resources.size(), 2);
	}

	@Override
	public int getPriority() {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
