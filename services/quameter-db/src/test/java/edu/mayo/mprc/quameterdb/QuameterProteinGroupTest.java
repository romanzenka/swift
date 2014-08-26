package edu.mayo.mprc.quameterdb;

import com.google.common.collect.Lists;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class QuameterProteinGroupTest {
	@Test
	public void shouldMatchGroup() {
		final QuameterProteinGroup group = new QuameterProteinGroup("hello", "ALBU_HUMAN|IGLC1_.*_HUMAN|IGKV\\d+_HUMAN");
		Assert.assertTrue(group.matches(Lists.newArrayList("HELLO_WORLD", "IGKV12345_HUMAN")), "Matches because of IGKV");
	}

	@Test
	public void shouldNotMatchPartially() {
		final QuameterProteinGroup group = new QuameterProteinGroup("hello", "ALBU_HUMAN|IGLC1_.*_HUMAN|IGKV\\d+_HUMAN");
		Assert.assertFalse(group.matches(Lists.newArrayList("BU_HU", "GKV12345_HUMAN", "IGLC1_HI_HUMA")), "Should not match partially");
	}

}
