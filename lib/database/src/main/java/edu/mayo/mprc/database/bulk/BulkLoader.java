package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.database.SessionProvider;
import org.hibernate.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;

/**
 * Load a large list of values into the database.
 * <p/>
 * If the value already exist - use the existing one.
 * <p/>
 * If the value does not match existing - create new record.
 * <p/>
 * Fill in ids of all the values as they are in the database.
 * <p/>
 * Finally, and most importantly - do this FAST.
 *
 * @author Roman Zenka
 */
public abstract class BulkLoader<T extends PersistableBase> {
	public static final int BATCH_SIZE = 100;

	private final BulkLoadJobStarter jobStarter;
	private final SessionProvider sessionProvider;

	protected BulkLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
		this.jobStarter = jobStarter;
		this.sessionProvider = sessionProvider;
	}

	/**
	 * @return name of the temporary table. The table must contain 'new_id', 'data_order'
	 */
	public abstract String getTempTableName();

	public abstract String getTableName();

	public String getTableIdColumn() {
		return getTableName() + "_id";
	}

	public abstract String getEqualityString();

	/**
	 * @param value value to wrap
	 * @return wrapped value
	 */
	public abstract Object wrapForTempTable(T value, TempKey key);

	/**
	 * Comma separated list of columns to transfer from temp table to the actual one.
	 */
	public abstract String getColumnsToTransfer();

	public void addObjects(final Collection<? extends T> values) {
		final BulkLoadJob bulkLoadJob = jobStarter.startNewJob();

		// Load data quickly into temp table
		final int numAddedValues = loadTempValues(values, bulkLoadJob);

		if (numAddedValues > 0) {
			final int recordsUpdated;
			try {
				recordsUpdated = updateExisting(bulkLoadJob);
			} catch (Exception e) {
				throw new MprcException("Bulk update step 1 failed", e);
			}

			if (recordsUpdated != numAddedValues) {
				throw new MprcException(MessageFormat.format("Programmer error: we were supposed to update {0}, instead updated {1}",
						numAddedValues, recordsUpdated));
			}

			final int lastId = getLastId();

			try {
				insertMissing(bulkLoadJob, lastId);
			} catch (Exception e) {
				throw new MprcException("Bulk insert step 2 failed", e);
			}

			getSession().flush();
			getSession().clear();

			loadNewIdsBack(values, bulkLoadJob);

			deleteTemp(bulkLoadJob, numAddedValues);
		}

		jobStarter.endJob(bulkLoadJob);
	}

	protected int loadTempValues(final Collection<? extends T> values, final BulkLoadJob bulkLoadJob) {
		int order = 0;
		int numAddedValues = 0;
		for (final T value : values) {
			if (value.getId() == null) {
				order++;
				final TempKey key = new TempKey(bulkLoadJob.getId(), order);
				final Object load = wrapForTempTable(value, key);
				getSession().save(load);
				getSession().setReadOnly(load, true);
				numAddedValues++;
				if (order % BATCH_SIZE == 0) {
					getSession().flush();
					getSession().clear();
				}
			}
		}
		getSession().flush();
		getSession().clear();
		return numAddedValues;
	}

	protected int updateExisting(final BulkLoadJob bulkLoadJob) {
		final String table = getTableName();
		final String tableId = getTableIdColumn();
		final String tempTableName = getTempTableName();
		final String equalityString = getEqualityString();

		final SQLQuery sqlQuery = getSession().createSQLQuery("UPDATE " + tempTableName + " AS t SET t.new_id = (select s." + tableId + " from " + table + " as s where " + equalityString + " and t.job = :job LIMIT 1) where t.job = :job");
		sqlQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		final int update1 = sqlQuery.executeUpdate();
		return update1;
	}

	protected int getLastId() {
		final String table = getTableName();
		final String tableId = getTableIdColumn();

		try {
			final Integer lastId = (Integer) getSession()
					.createSQLQuery("select max(" + tableId + ") from " + table)
					.uniqueResult();
			if (lastId == null) {
				return 0;
			}
			return lastId;
		} catch (Exception e) {
			throw new MprcException("Could not determine last id in table " + table, e);
		}
	}

	protected void insertMissing(final BulkLoadJob bulkLoadJob, final int lastId) {
		final String table = getTableName();
		final String tableId = getTableIdColumn();
		final String tempTableName = getTempTableName();
		final String columnsToTranfer = getColumnsToTransfer();
		try {
			final Query query = getSession()
					.createSQLQuery(
							MessageFormat.format(
									"INSERT INTO {0} ({1}, {2}) select data_order+{3,number,#}, {4} from {5} where job = :job and new_id is null",
									table, tableId, columnsToTranfer, lastId, columnsToTranfer, tempTableName))
					.setParameter("job", bulkLoadJob.getId());
			query.executeUpdate();
		} catch (Exception e) {
			throw new MprcException("Cannot insert new data to " + table, e);
		}

		try {
			final Query query2 = getSession()
					.createSQLQuery(
							MessageFormat.format(
									"UPDATE {0} SET new_id = data_order+{1,number,#} where job = :job and new_id is null",
									tempTableName, lastId))
					.setParameter("job", bulkLoadJob.getId());
			query2.executeUpdate();
		} catch (Exception e) {
			throw new MprcException("Cannot update existing data in " + tempTableName, e);
		}
	}

	protected void loadNewIdsBack(final Collection<? extends T> values, final BulkLoadJob bulkLoadJob) {
		final String tempTableName = getTempTableName();
		final Query query = getSession().createSQLQuery(
				MessageFormat.format(
						"SELECT new_id FROM {0} t WHERE t.job = :job ORDER BY t.data_order",
						tempTableName));

		query.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		query.setReadOnly(true);
		final ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY);
		final Iterator<? extends T> iterator = values.iterator();
		int rowNumber = 0;
		while (scroll.next()) {
			rowNumber++;
			final Integer newId = (Integer) scroll.get(0);
			final T value = nextNullIdValue(iterator);
			if (newId != null) {
				if (value == null) {
					throw new MprcException("Ran out of values before data finished streaming in! Two identical values must have been present in the collection. Row: " + rowNumber);
				}
				value.setId(newId);
			} else {
				throw new MprcException("All ids must be set, not true for row: " + rowNumber);
			}
		}
		scroll.close();
	}

	protected void deleteTemp(final BulkLoadJob bulkLoadJob, final int numAddedValues) {
		final String tempTableName = getTempTableName();
		final SQLQuery deleteQuery = getSession().createSQLQuery(
				MessageFormat.format(
						"DELETE FROM {0} WHERE job=:job", tempTableName));
		deleteQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		final int numDeleted = deleteQuery.executeUpdate();
		if (numDeleted != numAddedValues) {
			throw new MprcException("Could not delete all the elements from the temporary table");
		}
	}

	private T nextNullIdValue(final Iterator<? extends T> iterator) {
		T value = iterator.next();
		// Skip all the values already saved
		while (value != null && value.getId() != null) {
			value = iterator.next();
		}
		return value;
	}

	public Session getSession() {
		return sessionProvider.getSession();
	}
}
