package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.SessionProvider;

/**
 * @author Roman Zenka
 */
public final class SequenceBulkLoader extends BulkLoader<Sequence> {
	private final String tableName;

	public SequenceBulkLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider, final String tableName) {
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
		return "t.sequence = s.sequence";
	}

	@Override
	public Object wrapForTempTable(final Sequence value, final BulkLoadJob job, final int order) {
		return new TempSequenceLoading(job, order, value);
	}


	@Override
	public String getColumnsToTransfer() {
		return "sequence, mass";
	}
}
