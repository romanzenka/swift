package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.fastadb.BulkLoadJobStarter;
import edu.mayo.mprc.fastadb.BulkLoader;
import edu.mayo.mprc.fastadb.TempKey;

/**
 * @author Roman Zenka
 */
public final class LocalizedModificationLoader extends BulkLoader<LocalizedModification> {
	protected LocalizedModificationLoader(BulkLoadJobStarter jobStarter, SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public String getTempTableName() {
		return "temp_localized_modification";
	}

	@Override
	public String getTableName() {
		return "localized_modification";
	}

	@Override
	public String getEqualityString() {
		return "t.specificity_id = s.specificity_id and t.position = s.position and t.residue = s.residue";
	}

	@Override
	public Object wrapForTempTable(final LocalizedModification value, final TempKey key) {
		return new TempLocalizedModification(value, key);
	}

	@Override
	public String getColumnsToTransfer() {
		return "specificity_id, position, residue";
	}
}
