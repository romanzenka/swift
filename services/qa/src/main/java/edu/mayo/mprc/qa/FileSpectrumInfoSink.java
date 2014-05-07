package edu.mayo.mprc.qa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.msmseval.MSMSEvalOutputReader;
import edu.mayo.mprc.myrimatch.MyriMatchPepXmlReader;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldQaSpectraReader;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class FileSpectrumInfoSink implements SpectrumInfoSink {
	private File file;
	private FileWriter fileWriter;
	private String rawFileName;

	public FileSpectrumInfoSink(final File file) {
		this.file = file;
	}

	@Override
	public void initialize(final ScaffoldQaSpectraReader scaffold, final RawDumpReader rawDumpReader, final MSMSEvalOutputReader msmsEvalReader, final MyriMatchPepXmlReader myrimatchReader, final String rawFileName) {
		try {
			this.rawFileName = rawFileName;
			fileWriter = new FileWriter(file);

			fileWriter.write("Scan Id\tMz\tZ\tMgf File Name");
			if (scaffold != null) {
				fileWriter.write('\t');
				fileWriter.write(scaffold.getHeaderLine());
				fileWriter.write('\t');
				fileWriter.write("Scaffold version");
			}
			fileWriter.write('\t');
			fileWriter.write(msmsEvalReader.getHeaderLine());
			fileWriter.write('\t');
			if (rawFileName != null) {
				fileWriter.write("Raw File\t");
			}
			fileWriter.write(rawDumpReader.getHeaderLine());
			if (myrimatchReader != null) {
				fileWriter.write('\t');
				fileWriter.write(myrimatchReader.getHeaderLine());
			}
			fileWriter.write("\n");
		} catch (IOException e) {
			FileUtilities.closeQuietly(fileWriter);
			throw new MprcException(e);
		}
	}


	@Override
	public void writeSpectrumInfo(final String scanIdStr, final Spectrum spectrum, final String scaffoldInfo,
	                              final String scaffoldVersion,
	                              final String msmsEvalData, final String rawDumpReaderData,
	                              final String myrimatchReaderData) {
		try {
			fileWriter.write(scanIdStr
					+ "\t" + (spectrum != null ? spectrum.getMz() : "")
					+ "\t" + (spectrum != null ? spectrum.getCharge() : "")
					+ "\t" + (spectrum != null ? spectrum.getInputFileName() : "")
					+ (scaffoldInfo != null ? ("\t" + scaffoldInfo + "\t" + scaffoldVersion) : "")
					+ "\t");

			fileWriter.write(msmsEvalData);
			fileWriter.write('\t');
			if (rawFileName != null) {
				fileWriter.write(rawFileName);
				fileWriter.write('\t');
			}

			fileWriter.write(rawDumpReaderData);
			if (myrimatchReaderData != null) {
				fileWriter.write('\t');
				fileWriter.write(myrimatchReaderData);
			}
			fileWriter.write("\n");
		} catch (IOException e) {
			throw new MprcException(e);
		}
	}

	@Override
	public String getDescription() {
		return file.getAbsolutePath();
	}

	@Override
	public void close() throws IOException {
		if (fileWriter != null) {
			fileWriter.close();
		}
	}
}
