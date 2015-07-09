package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.bulk.BulkLoadJobStarter;
import edu.mayo.mprc.database.bulk.BulkLoader;
import edu.mayo.mprc.database.bulk.TempKey;

/**
 * @author Roman Zenka
 */
public final class ProteinAccnumBulkLoader extends BulkLoader<ProteinAccnum> {
	private final String tableName;

	public ProteinAccnumBulkLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider, final String tableName) {
		super(jobStarter, sessionProvider);
		this.tableName = tableName;
	}

	@Override
	public String getTempTableName() {
		return "temp_string_loading";
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public String getEqualityString() {
		return "<t>.data = <s>.accnum";
	}

	@Override
	public Object wrapForTempTable(final ProteinAccnum value, final TempKey key) {
		return new TempStringLoading(key, value.getAccnum());
	}

	@Override
	public String getColumnsFromTemp() {
		return "data";
	}

	@Override
	public String getColumnsToTarget() {
		return "accnum";
	}
}
