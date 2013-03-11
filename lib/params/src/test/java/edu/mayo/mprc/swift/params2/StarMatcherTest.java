package edu.mayo.mprc.swift.params2;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class StarMatcherTest {
	@Test
	public void testRegex() {
		final StarMatcher matcher = new StarMatcher("\\bADH1_YEAST\\b\n" +
				"\\bOVAL_CHICK\\b\n" +
				"\\bBGAL_ECOLI\\b\n" +
				"\\bLACB_BOVIN\\b",
				"\\s+",
				true);

		Assert.assertTrue(matcher.matches("OVAL_CHICK"));
		Assert.assertTrue(matcher.matches("LACB_BOVIN"));
		Assert.assertFalse(matcher.matches("APOe_HUMAN"));
	}

	@Test
	public void testDirect() {
		final StarMatcher matcher = new StarMatcher("ADH1_YEAST OVAL_CHICK  LACB_BOVIN  BGAL_ECOLI",
				" ",
				false);

		Assert.assertTrue(matcher.matches("adh1_yeast"));
		Assert.assertTrue(matcher.matches("LACB_BOVIN"));
		Assert.assertFalse(matcher.matches("APOE_HUMAN"));
	}

}
