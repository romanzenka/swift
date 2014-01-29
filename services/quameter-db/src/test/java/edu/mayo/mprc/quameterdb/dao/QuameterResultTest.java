package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class QuameterResultTest {
	@Test
	public void shouldGoToJson() {
		final QuameterResult result = new QuameterResult(null, null, new
				ImmutableMap.Builder<String, Double>()
				.put("MS2-4A", 1.33)
				.put("C-1A", 0.0)
				.build());
		Assert.assertEquals(result.getJsonData(), "{\"C-1A\":0.0,\"MS2-4A\":1.33}");
	}

	@Test
	public void shouldGoFromJson() {
		final QuameterResult result = new QuameterResult(null, null, null);
		result.setJsonData("{\"C-1A\":0.0,\"MS2-4A\":1.33}");
		Assert.assertEquals(result.getValues().get("C-1A"), 0.0);
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.33);
	}
}
