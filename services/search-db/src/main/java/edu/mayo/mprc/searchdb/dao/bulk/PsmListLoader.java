package edu.mayo.mprc.searchdb.dao.bulk;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.bulk.BulkHashedSetLoader;
import edu.mayo.mprc.database.bulk.BulkLoadJobStarter;
import edu.mayo.mprc.searchdb.dao.PsmList;

/**
 * @author Roman Zenka
 */
public final class PsmListLoader extends BulkHashedSetLoader<PsmList> {
	public PsmListLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public String getMemberTableName() {
		return "peptide_spectrum_match_list_members";
	}

	@Override
	public String getMemberTableValue() {
		return "peptide_spectrum_match_id";
	}

	@Override
	public String getTableName() {
		return "peptide_spectrum_match_list";
	}
}
