package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.bulk.BulkLoadJobStarter;
import edu.mayo.mprc.database.bulk.BulkLoader;
import edu.mayo.mprc.database.bulk.TempKey;
import org.hibernate.dialect.SQLServerDialect;

/**
 * @author Roman Zenka
 */
public final class SequenceBulkLoader extends BulkLoader<Sequence> {
	private final String tableName;
	// When true, we try to optimize the sequence queries on MSSQL server
	// We expect a calculated sequence_start column that is indexed on the table
	private final boolean msSqlOptimization;

	public SequenceBulkLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider, final String tableName) {
		super(jobStarter, sessionProvider);
		msSqlOptimization = sessionProvider.getDialect() instanceof SQLServerDialect;
		this.tableName = tableName;
	}

	@Override
	public String getTempTableName() {
		return "temp_sequence_loading";
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public String getEqualityString() {
		if (msSqlOptimization) {
			return "<t>.sequence = <s>.sequence AND LEFT(<t>.sequence, 200) = <s>.sequence_start";
		}
		return "<t>.sequence = <s>.sequence";
	}

	@Override
	public Object wrapForTempTable(final Sequence value, final TempKey key) {
		return new TempSequenceLoading(key, value);
	}


	@Override
	public String getColumnsToTransfer() {
		return "sequence, mass";
	}
}
