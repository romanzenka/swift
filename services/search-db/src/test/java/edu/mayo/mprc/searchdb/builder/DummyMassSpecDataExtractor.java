package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import org.joda.time.DateTime;

import java.io.File;

/**
 * Produces dummy information about mass spec files.
 *
 * @author Roman Zenka
 */
public class DummyMassSpecDataExtractor implements MassSpecDataExtractor {
	private DateTime now;

	public DummyMassSpecDataExtractor(final DateTime now) {
		this.now = now;
	}

	@Override
	public TandemMassSpectrometrySample getTandemMassSpectrometrySample(final String biologicalSampleName, final String msmsSampleName) {
		return new TandemMassSpectrometrySample(
				new File(msmsSampleName),
				now,
				0,
				0,
				0,
				"Dummy instrument",
				"Dummy #",
				now,
				0.0,
				"",
				"",
				"",
				"",
				""
		);
	}
}
