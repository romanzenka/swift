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
		return "temp_sequence_loading";
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public String getEqualityString() {
		return "<t>.sequence = <s>.accnum";
	}

	@Override
	public Object wrapForTempTable(final ProteinAccnum value, final TempKey key) {
		return new TempSequenceLoading(key, value);
	}

	@Override
	public String getColumnsToTransferFrom() {
		return "LEFT(sequence, 80)";
	}

	@Override
	public String getColumnsToTransferTo() {
		return "accnum";
	}
}
