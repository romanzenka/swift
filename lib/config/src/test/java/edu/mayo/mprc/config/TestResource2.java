package edu.mayo.mprc.config;

import org.testng.Assert;

/**
* @author Roman Zenka
*/
class TestResource2 implements ResourceConfig {

	@Override
	public void save(ConfigWriter writer) {
		writer.put("dummy", "dummyVal", "dummyComment");
	}

	@Override
	public void load(ConfigReader reader) {
		Assert.assertEquals(reader.get("dummy"), "dummyVal");
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
