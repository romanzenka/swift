package edu.mayo.mprc.searchdb.dao.bulk;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.bulk.BulkLoadJobStarter;
import edu.mayo.mprc.database.bulk.BulkLoader;
import edu.mayo.mprc.database.bulk.TempKey;
import edu.mayo.mprc.searchdb.dao.IdentifiedPeptide;
import edu.mayo.mprc.searchdb.dao.TempIdentifiedPeptide;

/**
 * @author Roman Zenka
 */
public final class IdentifiedPeptideLoader extends BulkLoader<IdentifiedPeptide> {
	public IdentifiedPeptideLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
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
	public Object wrapForTempTable(final IdentifiedPeptide value, final TempKey key) {
		return new TempIdentifiedPeptide(key, value);
	}

	@Override
	public String getColumnsToTransfer() {
		return "peptide_sequence_id, localized_mod_list_id";
	}
}
