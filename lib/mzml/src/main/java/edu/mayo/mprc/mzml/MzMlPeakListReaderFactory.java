package edu.mayo.mprc.mzml;

import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.peaklist.PeakListReaderFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Roman Zenka
 */
@Component("mzMlPeakListReaderFactory")
public final class MzMlPeakListReaderFactory implements PeakListReaderFactory {
	public MzMlPeakListReaderFactory() {
	}

	@Override
	public String getExtension() {
		return "mzML";
	}

	@Override
	public PeakListReader createReader(final File file) {
		return new MzMlPeakListReader(file);
	}
}
