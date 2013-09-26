package edu.mayo.mprc.scaffoldparser.spectra;

import com.google.common.base.Charsets;
import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author Roman Zenka
 */
public final class ScaffoldQaSpectraReaderTest {
	@Test(expectedExceptions = MprcException.class)
	public void shouldDetectMissingEOF() {
		try {
			final ScaffoldQaSpectraReader reader = new ScaffoldQaSpectraReader();
			final String input = "Scaffold\nBlah blah\n\tKey: Value\n\nExperiment name\tSpectrum name\tCol3\nData1\tData2\tData3\nData4\tData5\tData6\n";
			final byte[] bytes = input.getBytes(Charsets.US_ASCII);
			final InputStream stream = new ByteArrayInputStream(bytes);

			final long inputSize = bytes.length;
			reader.load(stream, inputSize, "huh", "3.6.2", null);
		} catch (MprcException e) {
			String message = MprcException.getDetailedMessage(e);
			Assert.assertTrue(message.contains("END OF FILE marker not found"),
					"The error message has to mention 'END OF FILE marker not found', was " + message);
			throw e;
		}
	}
}
