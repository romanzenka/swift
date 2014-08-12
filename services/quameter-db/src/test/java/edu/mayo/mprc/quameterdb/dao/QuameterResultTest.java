package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.regex.Pattern;

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
				.build(), 0);
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.22);
	}

	@Test
	public void shouldSetValues() {
		final QuameterResult result = new QuameterResult(null, null, null, 0);
		result.setValue("C-1A", 0.0);
		result.setValue("MS2-4A", 1.33);
		Assert.assertEquals(result.getValues().get("C-1A"), 0.0);
		Assert.assertEquals(result.getValues().get("MS2-4A"), 1.33);
	}

	@Test
	public void shouldMatchPatientNamedPost() {
		final QuameterResult result = new QuameterResult(null, null, null, 0);
		final FileSearch fileSearch = new FileSearch();
		fileSearch.setInputFile(new File("HR04-689_Post_20140806_Y30_QE.raw"));
		fileSearch.setExperiment("Test experiment name");
		result.setFileSearch(fileSearch);

		Assert.assertTrue(result.resultMatches(Pattern.compile(".*")), "Should match although the file contains Post as patient name");
	}

	@Test
	public void shouldNotMatchPre() {
		final QuameterResult result = new QuameterResult(null, null, null, 0);
		final FileSearch fileSearch = new FileSearch();
		fileSearch.setInputFile(new File("HR04-689_Post_20140806_Y30_QE_PreS1.raw"));
		fileSearch.setExperiment("Test experiment name");
		result.setFileSearch(fileSearch);

		Assert.assertFalse(result.resultMatches(Pattern.compile(".*")), "Should not match Pre file");
	}

}
