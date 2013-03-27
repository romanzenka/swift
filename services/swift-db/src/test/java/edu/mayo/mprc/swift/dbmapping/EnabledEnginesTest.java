package edu.mayo.mprc.swift.dbmapping;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class EnabledEnginesTest {
	@Test
	public void testGetEngineVersion() throws Exception {
		EnabledEngines engines = new EnabledEngines();
		engines.add(new SearchEngineConfig("MASCOT", "2.3"));
		engines.add(new SearchEngineConfig("SEQUEST", "v27"));
		engines.add(new SearchEngineConfig("MASCOT", "2.2"));

		Assert.assertEquals(engines.getEngineVersion("SEQUEST"), "v27");
		try {
			engines.getEngineVersion("MASCOT");
			Assert.fail("Exception should have been thrown");
		} catch(MprcException e) {
			Assert.assertTrue(e.getMessage().contains("versions: 2.2, 2.3"));
		}
	}
}
