package edu.mayo.mprc.peaklist;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all peak list readers supported.
 *
 * @author Roman Zenka
 */
@Component("peakListReaders")
public final class PeakListReaders {
	private final Map<String/*extension*/, PeakListReaderFactory> factories = new HashMap<String, PeakListReaderFactory>(2);

	public PeakListReaders() {
	}

	public PeakListReaders(final Collection<PeakListReaderFactory> factories) {
		setReaderFactories(factories);
	}

	public PeakListReader createReader(final File file, final boolean readPeaks) {
		final String extension = FileUtilities.getExtension(file.getName());
		final PeakListReaderFactory factory = factories.get(extension);
		if (factory == null) {
			throw new MprcException("No peak list reader able to read [" + file.getAbsolutePath() + "] - unsupported extension [" + extension + "]");
		}
		return factory.createReader(file, readPeaks);
	}

	public Collection<PeakListReaderFactory> getReaderFactories() {
		return factories.values();
	}

	@Autowired
	public void setReaderFactories(final Collection<PeakListReaderFactory> factories) {
		this.factories.clear();
		for (final PeakListReaderFactory factory : factories) {
			this.factories.put(factory.getExtension(), factory);
		}
	}
}
