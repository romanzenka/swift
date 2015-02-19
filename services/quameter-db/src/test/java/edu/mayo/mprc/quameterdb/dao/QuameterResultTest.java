package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.quameterdb.QuameterDaoHibernate;
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
				.build(), null);
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.22);
	}

	@Test
	public void shouldSetValues() {
		final QuameterResult result = new QuameterResult(null, null, null, null);
		result.setValue("C-1A", 0.0);
		result.setValue("MS2-4A", 1.33);
		Assert.assertEquals(result.getValues().get("C-1A"), 0.0);
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.33);
	}

	@Test
	public void shouldMatchPatientNamedPost() {
		Assert.assertFalse(QuameterDaoHibernate.PRE_POST.matcher("HR04-689_Post_20140806_Y30_QE.raw").find(), "Should match although the file contains Post as patient name");
	}

	@Test
	public void shouldNotMatchPre() {
		Assert.assertTrue(QuameterDaoHibernate.PRE_POST.matcher("HR04-689_Post_20140806_Y30_QE_PreS1.raw").find(), "Should not match Pre file");
	}

}
