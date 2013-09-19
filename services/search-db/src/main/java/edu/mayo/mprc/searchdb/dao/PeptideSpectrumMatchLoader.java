package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.fastadb.BulkLoadJobStarter;
import edu.mayo.mprc.fastadb.BulkLoader;
import edu.mayo.mprc.fastadb.TempKey;

/**
 * @author Roman Zenka
 */
public final class PeptideSpectrumMatchLoader extends BulkLoader<PeptideSpectrumMatch> {
	public PeptideSpectrumMatchLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public String getTempTableName() {
		return "temp_peptide_spectrum_match";
	}

	@Override
	public String getTableName() {
		return "peptide_spectrum_match";
	}

	@Override
	public String getEqualityString() {
		return "s.identified_peptide_id = t.identified_peptide_id AND " +
				"s.previous_aa = t.previous_aa AND " +
				"s.next_aa = t.next_aa AND " +
				"s.best_id_probability - t.best_id_probability < " + PeptideSpectrumMatch.PERCENT_TOLERANCE + " AND " +
				"t.best_id_probability - s.best_id_probability < " + PeptideSpectrumMatch.PERCENT_TOLERANCE + " AND " +
				"s.total_identified_spectra = t.total_identified_spectra AND " +
				"s.identified_1h_spectra = t.identified_1h_spectra AND " +
				"s.identified_2h_spectra = t.identified_2h_spectra AND " +
				"s.identified_3h_spectra = t.identified_3h_spectra AND " +
				"s.identified_4h_spectra = t.identified_4h_spectra AND " +
				"s.num_enzymatic_terminii = t.num_enzymatic_terminii";
	}

	@Override
	public Object wrapForTempTable(final PeptideSpectrumMatch value, final TempKey key) {
		return new TempPeptideSpectrumMatch(key, value);
	}

	@Override
	public String getColumnsToTransfer() {
		return "identified_peptide_id, previous_aa, next_aa, best_id_probability, total_identified_spectra, identified_1h_spectra, identified_2h_spectra, identified_3h_spectra, identified_4h_spectra, num_enzymatic_terminii";
	}
}
