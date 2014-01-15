package edu.mayo.mprc.io.mgf;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class MgfFilterTest {

	@Test
	public void testSpectrumTitleExtraction() {

		final SpectrumNumberExtractor extractor = new SpectrumNumberExtractor();
		Assert.assertEquals(
				extractor.extractSpectrumNumberFromTitle(" TITLE=test1 scan 10 10 (test1.10.10.3.dta) "),
				10,
				"The spectrum number is not extracted correctly.");

		Assert.assertEquals(
				extractor.extractSpectrumNumberFromTitle("TITLE=test1 scan 10 10 ( test1.20.20.3.dta ) "),
				20,
				"The spectrum number is not extracted correctly.");

		Assert.assertEquals(
				extractor.extractSpectrumNumberFromTitle("controllerType=0 controllerNumber=1 scan=4"),
				4,
				"The spectrum number is not extracted correctly.");


	}
}
