package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.fastadb.BulkLoadJobStarter;
import edu.mayo.mprc.fastadb.BulkLoader;
import edu.mayo.mprc.fastadb.TempKey;

/**
 * @author Roman Zenka
 */
public final class IdentifiedPeptideLoader extends BulkLoader<IdentifiedPeptide> {
	protected IdentifiedPeptideLoader(BulkLoadJobStarter jobStarter, SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public String getTempTableName() {
		return "temp_identified_peptide";
	}

	@Override
	public String getTableName() {
		return "identified_peptide";
	}

	@Override
	public String getEqualityString() {
		return "s.peptide_sequence_id=t.peptide_sequence_id and s.localized_mod_list_id = t.localized_mod_list_id";
	}

	@Override
	public Object wrapForTempTable(IdentifiedPeptide value, TempKey key) {
		return new TempIdentifiedPeptide(key, value);
	}

	@Override
	public String getColumnsToTransfer() {
		return "peptide_sequence_id, localized_mod_list_id";
	}
}
