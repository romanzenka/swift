package edu.mayo.mprc.qa;

import edu.mayo.mprc.msmseval.MSMSEvalOutputReader;
import edu.mayo.mprc.myrimatch.MyriMatchPepXmlReader;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldQaSpectraReader;

import java.io.Closeable;

/**
 * @author Roman Zenka
 */
public interface SpectrumInfoSink extends Closeable {
	void initialize(
			ScaffoldQaSpectraReader scaffold,
			RawDumpReader rawDumpReader,
			MSMSEvalOutputReader msmsEvalReader,
			MyriMatchPepXmlReader myrimatchReader,
			UvDataReader uvDataReader,
			String rawFileName);

	void writeSpectrumInfo(
			String scanIdStr,
			Spectrum spectrum,
			String scaffoldInfo,
			String scaffoldVersion,
			String msmsEvalData,
			String rawDumpReaderData,
			String myrimatchReaderData,
	        String uvDataReaderData
	);

	/**
	 * Describes this sink for debugging purposes.
	 */
	String getDescription();
}
