package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.fastadb.PeptideSequence;
import edu.mayo.mprc.searchdb.dao.IdentifiedPeptide;
import edu.mayo.mprc.searchdb.dao.PeptideSpectrumMatch;
import edu.mayo.mprc.searchdb.dao.PsmList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public class PsmListBuilder implements Builder<PsmList> {
	private static final int EXPECTED_PEPTIDES_PER_PROTEIN = 5;

	private Map<IdentifiedPeptide, PeptideSpectrumMatchBuilder> list = new LinkedHashMap<IdentifiedPeptide, PeptideSpectrumMatchBuilder>(EXPECTED_PEPTIDES_PER_PROTEIN);

	private ProteinGroupBuilder proteinGroup;
	private AnalysisBuilder analysis;

	public PsmListBuilder(final ProteinGroupBuilder proteinGroup) {
		this.proteinGroup = proteinGroup;
		analysis = proteinGroup.getSearchResult().getBiologicalSample().getAnalysis();
	}

	@Override
	public PsmList build() {
		final List<PeptideSpectrumMatch> items = new ArrayList<PeptideSpectrumMatch>(list.size());
		for (final PeptideSpectrumMatchBuilder builder : list.values()) {
			items.add(builder.build());
		}
		return new PsmList(items);
	}

	public ProteinGroupBuilder getProteinGroup() {
		return proteinGroup;
	}

	/**
	 * Get current {@link PeptideSpectrumMatch} entry. If none exist, new one is created and added to the protein group.
	 *
	 * @param peptideSequence           Peptide sequence. This + fixed+variable mods form a primary key.
	 * @param fixedModifications        Fixed modifications, parsed by {@link edu.mayo.mprc.searchdb.ScaffoldModificationFormat}. Primary key.
	 * @param variableModifications     Variable modifications, parsed by {@link edu.mayo.mprc.searchdb.ScaffoldModificationFormat}. Primary key.
	 * @param previousAminoAcid         Previous amino acid in the context of this protein group.
	 * @param nextAminoAcid             Next amino acid in the context of this protein group.
	 * @param numberOfEnzymaticTerminii Number of enzymatic terminii for this peptide (0,1=semi,2=fully)
	 * @return Current peptide spectrum match information.
	 */
	public PeptideSpectrumMatchBuilder getPeptideSpectrumMatch(
			final String peptideSequence,
			final String fixedModifications,
			final String variableModifications,
			final char previousAminoAcid,
			final char nextAminoAcid,
			final int numberOfEnzymaticTerminii) {

		final PeptideSequence sequence = analysis.getPeptideSequence(peptideSequence);
		final IdentifiedPeptide identifiedPeptide = analysis.getIdentifiedPeptide(sequence, fixedModifications, variableModifications);
		final PeptideSpectrumMatchBuilder match = list.get(identifiedPeptide);
		if (match == null) {
			final PeptideSpectrumMatchBuilder newMatch = new PeptideSpectrumMatchBuilder(analysis);
			newMatch.setPeptide(identifiedPeptide);
			newMatch.setPreviousAminoAcid(previousAminoAcid);
			newMatch.setNextAminoAcid(nextAminoAcid);
			newMatch.setNumberOfEnzymaticTerminii(numberOfEnzymaticTerminii);
			list.put(identifiedPeptide, newMatch);
			return newMatch;
		}
		return match;
	}
}
