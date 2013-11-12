package edu.mayo.mprc.searchdb.bulk;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.bulk.BulkHashedSetLoader;
import edu.mayo.mprc.database.bulk.BulkLoadJobStarter;
import edu.mayo.mprc.searchdb.dao.ProteinSequenceList;

/**
 * @author Roman Zenka
 */
public final class ProteinSequenceListLoader extends BulkHashedSetLoader<ProteinSequenceList> {
	public ProteinSequenceListLoader(BulkLoadJobStarter jobStarter, SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public String getMemberTableName() {
		return "protein_sequence_list_members";
	}

	@Override
	public String getMemberTableValue() {
		return "protein_sequence_id";
	}

	@Override
	public String getTableName() {
		return "protein_sequence_list";
	}
}
