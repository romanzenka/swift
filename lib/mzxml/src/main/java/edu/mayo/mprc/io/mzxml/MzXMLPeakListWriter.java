/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mayo.mprc.io.mzxml;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.proteomecommons.io.Peak;
import org.proteomecommons.io.PeakList;
import org.proteomecommons.io.mzxml.Base64;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for creating mzXML files given peak list objects.
 */
public final class MzXMLPeakListWriter implements Closeable {

	private FileWriter fileWriter;
	private XMLStreamWriter xmlWriter;
	private boolean enable64BitPrecision;
	private int scanNumber;

	/**
	 * @param outputFileName
	 * @throws javax.xml.stream.XMLStreamException
	 *
	 * @throws java.io.IOException
	 */
	public MzXMLPeakListWriter(final String outputFileName) throws XMLStreamException, IOException {
		this(outputFileName, true);
	}

	/**
	 * @param outputFile
	 * @throws javax.xml.stream.XMLStreamException
	 *
	 * @throws java.io.IOException
	 */
	public MzXMLPeakListWriter(final File outputFile) throws XMLStreamException, IOException {
		this(outputFile, true);
	}

	/**
	 * @param outputFileName
	 * @param enable64BitPrecision
	 * @throws javax.xml.stream.XMLStreamException
	 *
	 * @throws java.io.IOException
	 */
	public MzXMLPeakListWriter(final String outputFileName, final boolean enable64BitPrecision) throws XMLStreamException, IOException {
		this(new File(outputFileName), enable64BitPrecision);
	}

	/**
	 * @param outputFile
	 * @param enable64BitPrecision
	 * @throws javax.xml.stream.XMLStreamException
	 *
	 * @throws java.io.IOException
	 */
	public MzXMLPeakListWriter(final File outputFile, final boolean enable64BitPrecision) throws XMLStreamException, IOException {

		this.enable64BitPrecision = enable64BitPrecision;

		/**
		 * Scan number must start at 1
		 */
		scanNumber = 1;

		/**
		 * Check if file exists. If file does not exist, create it
		 */
		FileUtilities.ensureFileExists(outputFile);

		fileWriter = new FileWriter(outputFile);
		xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(fileWriter);

		/**
		 * Generate header for the mzXML file.
		 */
		xmlWriter.writeStartDocument();
		xmlWriter.writeCharacters("\n");
		xmlWriter.writeStartElement("mzXML");
		xmlWriter.writeAttribute("xmlns", "http://sashimi.sourceforge.net/schema_revision/mzXML_2.1");
		xmlWriter.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		xmlWriter.writeAttribute("xsi:schemaLocation", "http://sashimi.sourceforge.net/schema_revision/mzXML_2.1 http://sashimi.sourceforge.net/schema_revision/mzXML_2.1/mzXML_idx_2.1.xsd");
		xmlWriter.writeCharacters("\n");
		xmlWriter.writeStartElement("msRun");
	}

	public int writePeakList(final PeakList peakList) throws XMLStreamException {
		final Peak[] peaks = peakList.getPeaks();

		int byteBufferCapacity = 0;

		/**
		 * bytes needed = (bits per double/bits per byte)*2 double per peak*number of peaks
		 */
		if (enable64BitPrecision) {
			byteBufferCapacity = (64 / 8) * 2 * peaks.length;
		} else {
			byteBufferCapacity = (32 / 8) * 2 * peaks.length;
		}

		ByteBuffer bb = ByteBuffer.allocate(byteBufferCapacity);

		/**
		 * Network byte order is big endian http://mindprod.com/jgloss/endian.html
		 */
		bb = bb.order(ByteOrder.BIG_ENDIAN);

		for (final Peak peak : peaks) {
			if (!enable64BitPrecision) {
				bb.putFloat((float) peak.getMassOverCharge());
				bb.putFloat((float) peak.getIntensity());
			} else {
				bb.putDouble(peak.getMassOverCharge());
				bb.putDouble(peak.getIntensity());
			}
		}

		/**
		 * Convert bytes
		 */
		final String encodedBytes = Base64.encodeBytes(bb.array(), false);

		xmlWriter.writeCharacters("\n");
		xmlWriter.writeStartElement("scan");
		xmlWriter.writeAttribute("num", "" + scanNumber);

		scanNumber++;

		if (peakList.getTandemCount() != PeakList.UNKNOWN_TANDEM_COUNT) {
			xmlWriter.writeAttribute("msLevel", "" + peakList.getTandemCount());
		}

		xmlWriter.writeAttribute("peaksCount", "" + peaks.length);

		/**
		 * Write precursor mz info
		 */
		if (peakList.getParentPeak() != null) {
			final Peak parentPeak = peakList.getParentPeak();

			if (parentPeak.getIntensity() == Peak.UNKNOWN_INTENSITY) {
				throw new IllegalArgumentException("mzXML requires an intensity value for precursor peaks.");
			}

			if (parentPeak.getMassOverCharge() == Peak.UNKNOWN_MZ) {
				throw new IllegalArgumentException("mzXML requires an mz value for precursor peaks.");
			}

			xmlWriter.writeCharacters("\n");
			xmlWriter.writeStartElement("precursorMz");

			writeNonNull("precursorIntensity", Float.toString((float) parentPeak.getIntensity()));

			if (parentPeak.getCharge() != Peak.UNKNOWN_CHARGE) {
				xmlWriter.writeAttribute("precursorCharge", Integer.toString(parentPeak.getCharge()));
			}

			xmlWriter.writeCharacters(Float.toString((float) parentPeak.getMassOverCharge()));

			/**
			 * Close the precursorMz element
			 */
			xmlWriter.writeEndElement();
		} else {
			throw new IllegalArgumentException("mzXML requires an intensity and mz values for precursor peaks.");
		}

		/**
		 * Setup peaks element
		 */
		xmlWriter.writeCharacters("\n");
		xmlWriter.writeStartElement("peaks");

		if (!enable64BitPrecision) {
			xmlWriter.writeAttribute("precision", "32");
		} else {
			xmlWriter.writeAttribute("precision", "64");
		}

		/**
		 * These two values are always the same
		 */
		xmlWriter.writeAttribute("byteOrder", "network");
		xmlWriter.writeAttribute("pairOrder", "m/z-int");
		xmlWriter.writeCharacters(encodedBytes);

		/**
		 * End peaks and scan
		 */
		xmlWriter.writeCharacters("\n");
		xmlWriter.writeEndElement();
		xmlWriter.writeCharacters("\n");
		xmlWriter.writeEndElement();

		return scanNumber - 1;
	}

	private void writeNonNull(final String name, final String value) throws XMLStreamException {
		if (name != null && value != null) {
			xmlWriter.writeAttribute(name, value);
		}
	}

	public void close() {
		if (xmlWriter != null) {
			try {
				xmlWriter.writeEndElement();
				xmlWriter.close();
				xmlWriter = null;
			} catch (XMLStreamException e) {
				throw new MprcException("Could not close mzXML writer.", e);
			}
		}
		FileUtilities.closeQuietly(fileWriter);
	}
}
