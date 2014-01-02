package edu.mayo.mprc.io.mgf;

import edu.mayo.mprc.MprcException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts spectrum number from the mgf peak list.
 * There can eventually be several implementation of this, since different .mgf files might use different numbering conventions.
 */
public final class SpectrumNumberExtractor {
	private static final Pattern TITLE_SPECTRUM_NUMBER = Pattern.compile("\\(.*\\.(\\d+)\\.\\d+\\.\\d+\\.dta\\s*\\)\\s*$");
	private static final String WRONG_TITLE_MSG = "The .mgf title does not denote a proper spectrum number.\n" +
			"We expect the TITLE to end in ([filename].[spectrum_from].[spectrum_to].[charge].dta)\n" +
			"The spectrum title was:\n\t";

	public int extractSpectrumNumberFromTitle(final String title) {
		final Matcher m = TITLE_SPECTRUM_NUMBER.matcher(title);
		if (!m.find()) {
			throw new MprcException(WRONG_TITLE_MSG + title);
		}

		final String spectrum = m.group(1);
		int spectrumNumber = 0;
		try {
			spectrumNumber = Integer.parseInt(spectrum);
		} catch (NumberFormatException e) {
			throw new MprcException(WRONG_TITLE_MSG + title, e);
		}
		return spectrumNumber;
	}
}
