package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.bulk.BulkLoadJobStarter;
import edu.mayo.mprc.database.bulk.BulkLoader;
import edu.mayo.mprc.database.bulk.TempKey;

/**
 * @author Roman Zenka
 */
public final class ProteinDescriptionBulkLoader extends BulkLoader<ProteinDescription> {
	private final String tableName;

	public ProteinDescriptionBulkLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider, final String tableName) {
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
		return "<t>.data = <s>.description";
	}

	@Override
	public Object wrapForTempTable(final ProteinDescription value, final TempKey key) {
		return new TempStringLoading(key, value.getDescription());
	}

	@Override
	public String getColumnsFromTemp() {
		return "data";
	}

	@Override
	public String getColumnsToTarget() {
		return "description";
	}

}
