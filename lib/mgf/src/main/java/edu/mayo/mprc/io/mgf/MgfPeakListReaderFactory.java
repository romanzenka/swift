package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.peaklist.PeakListReaderFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Roman Zenka
 */
@Component("mgfPeakListReaderFactory")
public final class MgfPeakListReaderFactory implements PeakListReaderFactory {
	@Override
	public String getExtension() {
		return "mgf";
	}

	@Override
	public PeakListReader createReader(final File file) {
		return new MGFPeakListReader(file);
	}
}
