/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.utilities.FileUtilities;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.io.File;

/**
 * Filters spectra from an mgf file, separating "good" from "bad" ones.
 */
public final class MGFFilteredFileGenerator {

	private MGFFilteredFileGenerator() {
	}

	/**
	 * Separates spectra from given input .mgf files into two files (accepted, rejected), based on a filter.
	 * If either of the files is null, the spectra that would go into that file are lost.
	 *
	 * @param sourceMgf   Input mgf file. This file will not be modified in any way.
	 * @param acceptedMgf Accepted spectra mgf (for spectra where filter returns true)
	 * @param rejectedMgf Rejected spectra mgf (for spectra where filter returns false).
	 * @param filter      Filter that either accepts or rejects each particular spectrum.
	 */
	public static MgfFilteredSpectraCount filterMgfFile(final File sourceMgf, final File acceptedMgf, final File rejectedMgf, final MgfPeakListFilter filter) {
		int totalSpectra = 0;
		int acceptedSpectra = 0;
		int rejectedSpectra = 0;

		MGFPeakListReader sourceMgfReader = null;
		MGFPeakListWriter acceptedMgfWriter = null;
		MGFPeakListWriter rejectedMgfWriter = null;

		try {
			sourceMgfReader = new MGFPeakListReader(sourceMgf);
			if (acceptedMgf != null) {
				acceptedMgfWriter = new MGFPeakListWriter(acceptedMgf);
			}

			if (rejectedMgf != null) {
				rejectedMgfWriter = new MGFPeakListWriter(rejectedMgf);
			}

			MascotGenericFormatPeakList peakList = null;

			while ((peakList = sourceMgfReader.nextPeakList()) != null) {
				totalSpectra++;
				if (filter.peakListAccepted(peakList)) {
					acceptedSpectra++;
					if (acceptedMgfWriter != null) {
						acceptedMgfWriter.writePeakList(peakList);
					}
				} else {
					rejectedSpectra++;
					if (rejectedMgfWriter != null) {
						rejectedMgfWriter.writePeakList(peakList);
					}
				}
			}

		} finally {
			FileUtilities.closeQuietly(acceptedMgfWriter);
			FileUtilities.closeQuietly(rejectedMgfWriter);
			FileUtilities.closeQuietly(sourceMgfReader);
		}

		return new MgfFilteredSpectraCount(totalSpectra, acceptedSpectra, rejectedSpectra);
	}
}
