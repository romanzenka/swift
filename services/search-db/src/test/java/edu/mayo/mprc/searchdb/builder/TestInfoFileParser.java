package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.utilities.ResourceUtilities;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.Reader;

/**
 * Checks that we can parse the .RAW.info.tsv files properly.
 *
 * @author Roman Zenka
 */
public class TestInfoFileParser {
	@Test
	public void shouldParseInfoFile() {
		final InfoFileParser parser = new InfoFileParser();
		final Reader reader = ResourceUtilities.getReader("classpath:edu/mayo/mprc/searchdb/info.tsv", TestInfoFileParser.class);
		final InfoFileData data = parser.parse(reader);

		Assert.assertEquals(data.getMs1Spectra(), 2334);
		Assert.assertEquals(data.getMs2Spectra(), 6626);
		Assert.assertEquals(data.getMs3PlusSpectra(), 1);
		Assert.assertEquals(data.getInstrumentName(), "LTQ Orbitrap");
		Assert.assertEquals(data.getInstrumentSerialNumber(), "1063B");
		Assert.assertEquals(data.getStartTime(), new DateTime(2010, 4, 8, 18, 17, 49, 0));
		Assert.assertEquals(data.getRunTimeInSeconds(), 4500.49);
		Assert.assertEquals(data.getComment(), "yeast TD 60K, top5, 1ug in 100uL, inj 5uL,");
		Assert.assertEquals(data.getSampleId(), "post Ch2&Ch1 flush; std config");
	}
}
