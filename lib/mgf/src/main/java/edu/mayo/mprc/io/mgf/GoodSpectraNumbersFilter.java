package edu.mayo.mprc.io.mgf;

import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.util.Collection;

/**
 * Filters out spectra whose number is on the list.
 * Spectrum number is determined from the TITLE= parameter.
 * The title is in format <code>TITLE=text text text... ([filename].[spectrum_from].[spectrum_to].[charge].dta)</code>
 * We use <code>spectrum_from</code> as our spectrum number. This should work for most of our .mgf files where
 * spectra are not combined by extract_msn.
 */
public final class GoodSpectraNumbersFilter implements MgfPeakListFilter {

	private Collection<Integer> allowedSpectra;
	private SpectrumNumberExtractor spectrumNumberExtractor;

	public GoodSpectraNumbersFilter(final Collection<Integer> allowedSpectra, final SpectrumNumberExtractor extractor) {
		this.allowedSpectra = allowedSpectra;
		this.spectrumNumberExtractor = extractor;
	}

	@Override
	public boolean peakListAccepted(final MascotGenericFormatPeakList peakList) {
		if (peakList == null || peakList.getTitle() == null) {
			return false;
		}
		final int number = spectrumNumberExtractor.extractSpectrumNumber(peakList);
		return allowedSpectra.contains(number);
	}
}
