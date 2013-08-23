package edu.mayo.mprc.io.mgf;

import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.util.Collection;

/**
 * Has a list of bad spectra titles. If a title is found in the list, the spectrum is marked as rejected.
 */
class BadSpectraTitleFilter implements MgfPeakListFilter {
	private Collection<String> badSpectraMGFTitles;

	public BadSpectraTitleFilter(final Collection<String> badSpectraMGFTitles) {
		this.badSpectraMGFTitles = badSpectraMGFTitles;
	}

	@Override
	public boolean peakListAccepted(final MascotGenericFormatPeakList peakList) {
		// The peak list is accepted if it cannot be removed from the collection of bad spectra titles
		return !badSpectraMGFTitles.remove(peakList.getTitle());
	}
}
