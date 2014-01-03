/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mayo.mprc.io.mgf;

import com.google.common.base.Preconditions;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.mzxml.MzXMLPeakListWriter;
import edu.mayo.mprc.peaklist.PeakList;
import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.peaklist.PeakListReaders;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that provides functionality for converting mgf or mzml files to mzXML files.
 */
@Component("mzXmlConverter")
public final class MzXmlConverter {
	private PeakListReaders readers;

	public MzXmlConverter() {
	}

	public MzXmlConverter(PeakListReaders readers) {
		this.readers = readers;
	}

	/**
	 * The implementation of this method converts the mgf/mzML file defined by the parameter inputFileName to
	 * a file of the mzXML type defined by the mzXMLOutputFileName.
	 *
	 * @param inputFileName        Mgf input file name.
	 * @param mzXMLOutputFileName  MzXML output file name.
	 * @param enable64BitPrecision Defines the level of presicion of the peak lists in the mzXML file.
	 *                             True is 64 bit presicion and false is a 32 bit presicion level.
	 * @return Returns a map where the keys are the scan ids in mzXML file and the values are the
	 *         corresponding spectra titles in the mgf file.
	 */
	public Map<Integer, String> convert(final String inputFileName, final String mzXMLOutputFileName, final boolean enable64BitPrecision) {

		return convert(new File(inputFileName), new File(mzXMLOutputFileName), enable64BitPrecision);
	}

	/**
	 * The implementation of this method converts the mgf/mzML file defined by the parameter inputFileName to
	 * a file of the mzXML type defined by the mzXMLOutputFileName.
	 *
	 * @param inputFile            Mgf input file name.
	 * @param mzXMLOutputFile      MzXML output file name.
	 * @param enable64BitPrecision Defines the level of presicion of the peak lists in the mzXML file.
	 *                             True is 64 bit presicion and false is a 32 bit presicion level.
	 * @return Returns a map where the keys are the scan ids in mzXML file and the values are the
	 *         corresponding spectra titles in the mgf file.
	 */
	public Map<Integer, String> convert(final File inputFile, final File mzXMLOutputFile, final boolean enable64BitPrecision) {

		Preconditions.checkArgument(inputFile != null, "inputFile parameter can not be null.");
		Preconditions.checkArgument(mzXMLOutputFile != null, "mzXMLOutputFileName parameter can not be null.");

		/**
		 * Reader and writer objects.
		 */
		MzXMLPeakListWriter mzXMLWriter = null;
		PeakListReader inputReader = null;
		final Map<Integer, String> mzXMLScanToMGFTitle = new HashMap<Integer, String>(1000);

		try {
			inputReader = readers.createReader(inputFile);
			mzXMLWriter = new MzXMLPeakListWriter(mzXMLOutputFile, enable64BitPrecision);

			PeakList peakList = null;

			while ((peakList = inputReader.nextPeakList()) != null) {
				mzXMLScanToMGFTitle.put(mzXMLWriter.writePeakList(peakList), peakList.getTitle());
			}
		} catch (Exception t) {
			throw new MprcException("Conversion of " + inputFile.getAbsolutePath() + " to " + mzXMLOutputFile.getAbsolutePath() + " failed.", t);
		} finally {
			FileUtilities.closeQuietly(mzXMLWriter);
			FileUtilities.closeQuietly(inputReader);
		}

		Logger.getLogger(MzXmlConverter.class).log(Level.DEBUG, "File of type mzxml created. [" + mzXMLOutputFile.getAbsolutePath() + "]");

		return mzXMLScanToMGFTitle;
	}

	public PeakListReaders getReaders() {
		return readers;
	}

	@Resource(name = "peakListReaders")
	public void setReaders(final PeakListReaders readers) {
		this.readers = readers;
	}
}
