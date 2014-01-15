package edu.mayo.mprc.io.mgf;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.peaklist.PeakListReaderFactory;
import edu.mayo.mprc.peaklist.PeakListReaders;
import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Roman Zenka
 */
public final class TestMzXmlConverter {
	private File temp;
	private File inputMgf;
	private File outputMzXml;

	@BeforeTest
	public void setup() throws IOException {
		temp = FileUtilities.createTempFolder();
		inputMgf = new File(temp, "test.mgf");
		outputMzXml = new File(temp, "test.mzxml");
		Files.write(
				"BEGIN IONS\n" +
						"TITLE=test scan 1 1 (test.1.1.3.dta)\n" +
						"CHARGE=3+\n" +
						"PEPMASS=100.123\n" +
						"10.0 11.0\n" +
						"20.2 12.3\n" +
						"30.3 123456789.12345\n" +
						"END IONS\n" +
						"\n" +
						"BEGIN IONS\n" +
						"TITLE=test scan 2 2 (test.2.2.2.dta)\n" +
						"CHARGE=2+\n" +
						"PEPMASS=234.567\n" +
						"110.0 111.0\n" +
						"220.2 212.3\n" +
						"330.3 3123456789.12345\n" +
						"END IONS\n"
				, inputMgf, Charsets.US_ASCII);
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(temp);
	}

	@Test
	public void shouldConvertToMzXml() throws IOException {
		PeakListReaders readers = new PeakListReaders(Arrays.asList((PeakListReaderFactory) new MgfPeakListReaderFactory()));
		MzXmlConverter converter = new MzXmlConverter(readers);

		converter.convert(inputMgf, outputMzXml, true);

		String result = Files.toString(outputMzXml, Charsets.US_ASCII);

		Assert.assertEquals(result, "<?xml version=\"1.0\" ?>\n" +
				"<mzXML xmlns=\"http://sashimi.sourceforge.net/schema_revision/mzXML_2.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://sashimi.sourceforge.net/schema_revision/mzXML_2.1 http://sashimi.sourceforge.net/schema_revision/mzXML_2.1/mzXML_idx_2.1.xsd\">\n" +
				"<msRun>\n" +
				"<scan num=\"1\" msLevel=\"2\" peaksCount=\"3\">\n" +
				"<precursorMz precursorIntensity=\"0.0\" precursorCharge=\"3\">100.123</precursorMz>\n" +
				"<peaks precision=\"64\" byteOrder=\"network\" pairOrder=\"m/z-int\">QCQAAAAAAABAJgAAAAAAAEA0MzMzMzMzQCiZmZmZmZpAPkzMzMzMzUGdbzRUfmmt\n" +
				"</peaks>\n" +
				"</scan>\n" +
				"<scan num=\"2\" msLevel=\"2\" peaksCount=\"3\">\n" +
				"<precursorMz precursorIntensity=\"0.0\" precursorCharge=\"2\">234.567</precursorMz>\n" +
				"<peaks precision=\"64\" byteOrder=\"network\" pairOrder=\"m/z-int\">QFuAAAAAAABAW8AAAAAAAEBrhmZmZmZmQGqJmZmZmZpAdKTMzMzMzUHnRYVio/NN\n" +
				"</peaks>\n" +
				"</scan></msRun></mzXML>", "The output mzXML is different");
	}
}
