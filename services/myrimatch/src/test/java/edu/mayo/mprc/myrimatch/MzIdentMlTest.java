package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Roman Zenka
 */
public final class MzIdentMlTest extends XMLTestCase {
	@Test
	public void testReplace() throws Exception {
		XMLUnit.setIgnoreWhitespace(true);
		final File input = TestingUtilities.getTempFileFromResource(MzIdentMlTest.class, "test.mzid.xml", null);
		final File output = File.createTempFile("transformed", ".mzid");
		try {
			final ArrayList<String> titles = new ArrayList<String>(1);
			titles.add("spectrum 0");
			titles.add("spectrum 1");
			MzIdentMl.replace(input, titles, output);
			final String outputText = FileUtilities.readIntoString(FileUtilities.getReader(output), 1000000);
			assertXMLEqual(outputText,
					TestingUtilities.resourceToString("edu/mayo/mprc/myrimatch/expectedResult.mzid.xml"));
		} finally {
			FileUtilities.cleanupTempFile(input);
			FileUtilities.cleanupTempFile(output);
		}
	}
}
