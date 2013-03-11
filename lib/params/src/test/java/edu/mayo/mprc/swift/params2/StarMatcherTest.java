package edu.mayo.mprc.swift.params2;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class StarMatcherTest {
	@Test
	public void testRegex() {
		final StarMatcher m = new StarMatcher("\\bADH1_YEAST\\b\n" +
				"\\bOVAL_CHICK\\b\n" +
				"\\bBGAL_ECOLI\\b\n" +
				"\\bLACB_BOVIN\\b",
				"\\s+",
				true,
				true);

		Assert.assertTrue(m.matches("OVAL_CHICK"));
		Assert.assertTrue(m.matches("LACB_BOVIN"));
		Assert.assertFalse(m.matches("APOe_HUMAN"));
	}

	@Test
	public void testDirect() {
		final StarMatcher m = new StarMatcher("ADH1_YEAST OVAL_CHICK  LACB_BOVIN  BGAL_ECOLI",
				" ",
				false,
				true);

		Assert.assertTrue(m.matches("adh1_yeast"));
		Assert.assertTrue(m.matches("LACB_BOVIN"));
		Assert.assertFalse(m.matches("APOE_HUMAN"));
	}

}
