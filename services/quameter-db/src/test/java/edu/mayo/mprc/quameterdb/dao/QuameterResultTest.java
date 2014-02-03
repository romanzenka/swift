package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class QuameterResultTest {
	@Test
	public void shouldCreate() {
		final QuameterResult result = new QuameterResult(null, null, new
				ImmutableMap.Builder<String, Double>()
				.put("MS2-4A", 1.22)
				.put("C-1A", 0.0)
				.build());
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.22);
	}

	@Test
	public void shouldSetValues() {
		final QuameterResult result = new QuameterResult(null, null, null);
		result.setValue("C-1A", 0.0);
		result.setValue("MS2-4A", 1.33);
		Assert.assertEquals(result.getValues().get("C-1A"), 0.0);
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.33);
	}
}
