/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.proteomecommons.io.Peak;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for creating mgf files given MascotGenericFormatPeakList objects.
 */
public final class MGFPeakListWriter implements Closeable {

	private final BufferedWriter bufferedWriter;
	private final File outputFile;

	/**
	 * Constructor.
	 *
	 * @param outputFileName mgf file name.
	 * @throws IOException
	 */
	public MGFPeakListWriter(final String outputFileName) {
		this(new File(outputFileName));
	}

	/**
	 * Constructor.
	 *
	 * @param outputFile mgf file name.
	 * @throws IOException
	 */
	public MGFPeakListWriter(final File outputFile) {
		this.outputFile = outputFile;
		FileUtilities.ensureFolderExists(outputFile.getParentFile());
		bufferedWriter = new BufferedWriter(FileUtilities.getWriter(outputFile));
	}

	/**
	 * The implementation of this method writes the given parameter peak list to the
	 * mgf file represented by this object.
	 *
	 * @param peaklist
	 * @throws IOException
	 */
	public void writePeakList(final MascotGenericFormatPeakList peaklist) {

		if (peaklist == null) {
			throw new IllegalArgumentException("MascotGenericFormatPeakList peaklist can not be null.");
		}

		try {
			bufferedWriter.write("BEGIN IONS");
			bufferedWriter.newLine();

			if (peaklist.getCharge() != null) {
				bufferedWriter.write(peaklist.getCharge());
				bufferedWriter.newLine();
			} else {
				throw new IllegalArgumentException("MascotGenericFormatPeakList peaklist can not have charge value null.");
			}

			if (peaklist.getPepmass() != null) {
				bufferedWriter.write(peaklist.getPepmass());
				bufferedWriter.newLine();
			} else {
				throw new IllegalArgumentException("MascotGenericFormatPeakList peaklist can not have pepmass value null.");
			}

			if (peaklist.getTitle() != null) {
				bufferedWriter.write("TITLE=" + peaklist.getTitle());
				bufferedWriter.newLine();
			}

			// write out the peaks
			final Peak[] peaks = peaklist.getPeaks();
			for (final Peak peak : peaks) {
				bufferedWriter.write(peak.getMassOverCharge() + "\t" + peak.getIntensity());
				bufferedWriter.newLine();
			}

			bufferedWriter.write("END IONS");
			bufferedWriter.newLine();
			bufferedWriter.flush();
		} catch (IOException e) {
			throw new MprcException("Failure writing peaklist into .mgf " + outputFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Close the buffered writer object that handles this mgf file.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() {
		try {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		} catch (IOException e) {
			throw new MprcException("Failure closing .mgf file " + outputFile.getAbsolutePath(), e);
		}
	}
}
