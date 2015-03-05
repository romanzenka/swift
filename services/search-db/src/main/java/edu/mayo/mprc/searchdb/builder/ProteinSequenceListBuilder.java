package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.ProteinSequenceList;

import java.util.Collection;

/**
 * @author Roman Zenka
 */
public final class ProteinSequenceListBuilder implements Builder<ProteinSequenceList> {
	private final AnalysisBuilder analysis;
	private final Collection<String> accNums;

	public ProteinSequenceListBuilder(final Collection<String> accNums, final AnalysisBuilder analysis) {
		this.accNums = accNums;
		this.analysis = analysis;
	}

	@Override
	public ProteinSequenceList build() {
		final ProteinSequenceList proteinSequences = new ProteinSequenceList(accNums.size());
		for (final String accessionNumber : accNums) {
			proteinSequences.add(analysis.getProteinSequence(accessionNumber));
		}
		return proteinSequences;
	}

	public Collection<String> getAccNums() {
		return accNums;
	}
}
