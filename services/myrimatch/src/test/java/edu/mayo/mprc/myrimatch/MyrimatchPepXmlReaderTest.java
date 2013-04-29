package edu.mayo.mprc.myrimatch;

import com.google.common.base.Joiner;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;

public class MyriMatchPepXmlReaderTest {
	@Test
	public static void shouldParsePepXml() {
		final InputStream stream = ResourceUtilities.getStream("classpath:edu/mayo/mprc/myrimatch/result.pepXML", MyriMatchPepXmlReaderTest.class);
		final MyriMatchPepXmlReader reader = new MyriMatchPepXmlReader();
		reader.load(stream);
		final String line = reader.getLineForKey("3");

		Assert.assertEquals(line.replaceAll("[a-zA-Z0-9_.~+-]+", ""), reader.getEmptyLine(), "Empty line has same amount of tabs as normal line");
		Assert.assertEquals(reader.getHeaderLine(),
				"MyriMatch Peptide"
						+ '\t' + "MyriMatch Protein"
						+ '\t' + "MyriMatch Total Proteins"
						+ '\t' + "MyriMatch Num Matched Ions"
						+ '\t' + "MyriMatch Total Num Ions"
						+ '\t' + "MyriMatch mvh"
						+ '\t' + "MyriMatch mz Fidelity"
						+ '\t' + "MyriMatch xcorr", "MyriMatch header does not match");
		Assert.assertEquals(line, Joiner.on('\t').join(
				"SSGSSYPSLLQCLK", // peptide
				"CONTAM_TRYP_PIG", // protein
				"1", // total proteins
				"24", // matched ions
				"25", // total ions
				"112.38684~", // mvh
				"138.23982~", // mz fidelity
				"6.08570~" // xcorr
		), "MyriMatch reader read wrong data");
	}
}
