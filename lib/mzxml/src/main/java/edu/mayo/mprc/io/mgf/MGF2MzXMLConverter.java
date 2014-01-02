/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.mzxml.MzXMLPeakListWriter;
import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that provides functionality for converting mgf files to mzXML files.
 */
public final class MGF2MzXMLConverter {
	private MGF2MzXMLConverter() {
	}

	/**
	 * The implementation of this method converts the mgf file defined by the parameter mgfInputFileName to
	 * a file of the mzXML type defined by the mzXMLOutputFileName.
	 *
	 * @param mgfInputFileName     Mgf input file name.
	 * @param mzXMLOutputFileName  MzXML output file name.
	 * @param enable64BitPrecision Defines the level of presicion of the peak lists in the mzXML file.
	 *                             True is 64 bit presicion and false is a 32 bit presicion level.
	 * @return Returns a map where the keys are the scan ids in mzXML file and the values are the
	 *         corresponding spectra titles in the mgf file.
	 */
	public static Map<Integer, String> convert(final String mgfInputFileName, final String mzXMLOutputFileName, final boolean enable64BitPrecision) {

		return convert(new File(mgfInputFileName), new File(mzXMLOutputFileName), enable64BitPrecision);
	}

	/**
	 * The implementation of this method converts the mgf file defined by the parameter mgfInputFileName to
	 * a file of the mzXML type defined by the mzXMLOutputFileName.
	 *
	 * @param mgfInputFile         Mgf input file name.
	 * @param mzXMLOutputFile      MzXML output file name.
	 * @param enable64BitPrecision Defines the level of presicion of the peak lists in the mzXML file.
	 *                             True is 64 bit presicion and false is a 32 bit presicion level.
	 * @return Returns a map where the keys are the scan ids in mzXML file and the values are the
	 *         corresponding spectra titles in the mgf file.
	 */
	public static Map<Integer, String> convert(final File mgfInputFile, final File mzXMLOutputFile, final boolean enable64BitPrecision) {

		if (mgfInputFile == null) {
			throw new IllegalArgumentException("mgfInputFilename parameter can not be null.");
		} else if (mzXMLOutputFile == null) {
			throw new IllegalArgumentException("mzXMLOutputFileName parameter can not be null.");
		}

		/**
		 * Reader and writer objects.
		 */
		MzXMLPeakListWriter mzXMLWriter = null;
		PeakListReader mgfReader = null;
		final Map<Integer, String> mzXMLScanToMGFTitle = new HashMap<Integer, String>(1000);

		try {
			mgfReader = new MGFPeakListReader(mgfInputFile);
			mzXMLWriter = new MzXMLPeakListWriter(mzXMLOutputFile, enable64BitPrecision);

			MascotGenericFormatPeakList peakList = null;

			while ((peakList = mgfReader.nextPeakList()) != null) {
				mzXMLScanToMGFTitle.put(mzXMLWriter.writePeakList(peakList), peakList.getTitle());
			}
		} catch (Exception t) {
			throw new MprcException("Conversion of " + mgfInputFile.getAbsolutePath() + " to " + mzXMLOutputFile.getAbsolutePath() + " failed.", t);
		} finally {
			FileUtilities.closeQuietly(mzXMLWriter);
			FileUtilities.closeQuietly(mgfReader);
		}

		Logger.getLogger(MGF2MzXMLConverter.class).log(Level.DEBUG, "File of type mzxml created. [" + mzXMLOutputFile.getAbsolutePath() + "]");

		return mzXMLScanToMGFTitle;
	}
}
