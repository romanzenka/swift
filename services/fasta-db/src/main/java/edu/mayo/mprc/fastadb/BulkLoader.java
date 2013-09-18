package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.database.SessionProvider;
import org.hibernate.*;

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

	protected BulkLoader(BulkLoadJobStarter jobStarter, SessionProvider sessionProvider) {
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
	 * @param job   loading job
	 * @param order record order within the job
	 * @return wrapped value
	 */
	public abstract Object wrapForTempTable(T value, BulkLoadJob job, int order);

	/**
	 * Comma separated list of columns to transfer from temp table to the actual one.
	 *
	 * @return
	 */
	public abstract String getColumnsToTransfer();

	public void addObjects(final Collection<? extends T> values) {
		String table = getTableName();
		String tableId = getTableIdColumn();
		final BulkLoadJob bulkLoadJob = jobStarter.startNewJob();

		// Load data quickly into temp table

		int order = 0;
		int numAddedValues = 0;
		for (final T value : values) {
			if (value.getId() == null) {
				order++;
				final Object load = wrapForTempTable(value, bulkLoadJob, order);
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

		if (numAddedValues > 0) {
			String tempTableName = getTempTableName();
			String equalityString = getEqualityString();
			final SQLQuery sqlQuery = getSession().createSQLQuery("UPDATE " + tempTableName + " AS t SET t.new_id = (select s." + tableId + " from " + table + " as s where " + equalityString + " and t.job = :job)");
			sqlQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
			final int update1 = sqlQuery.executeUpdate();
			if (update1 != numAddedValues) {
				throw new MprcException("Programmer error: we were supposed to update " + numAddedValues + ", instead updated " + update1);
			}

			String columnsToTranfer = getColumnsToTransfer();
			final Query sqlQuery2 = getSession()
					.createSQLQuery("INSERT INTO " + table + " (" + columnsToTranfer + ") select " + columnsToTranfer + "  from " + tempTableName + " where job = :job and new_id is null")
					.setParameter("job", bulkLoadJob.getId());
			int insert1 = sqlQuery2.executeUpdate();

			if (insert1 > 0) {
				final SQLQuery sqlQuery3 = getSession().createSQLQuery("UPDATE " + tempTableName + " AS t SET t.new_id = (select s." + tableId + " from " + table + " as s where " + equalityString + " and t.job = :job) where t.new_id is null");
				sqlQuery3.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
				final int update2 = sqlQuery3.executeUpdate();
				if (update2 != insert1) {
					throw new MprcException("Programmer error: the amount of newly inserted values (" + update2 + ") does not match updated values (" + insert1 + ")");
				}
			}

			getSession().flush();
			getSession().clear();

			final Query query = getSession().createSQLQuery("select new_id from " + tempTableName + " t where t.job = :job order by t.data_order");
			query.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
			query.setReadOnly(true);
			final ScrollableResults scroll = query.scroll(ScrollMode.FORWARD_ONLY);
			final Iterator<? extends T> iterator = values.iterator();
			int rowNumber = 0;
			while (scroll.next()) {
				rowNumber++;
				final Integer newId = (Integer) scroll.get(0);
				T value = iterator.next();
				// Skip all the values already saved
				while (value != null && value.getId() != null) {
					value = iterator.next();
				}
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

			final SQLQuery deleteQuery = getSession().createSQLQuery("DELETE FROM " + tempTableName + " WHERE job=:job");
			deleteQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
			final int numDeleted = deleteQuery.executeUpdate();
			if (numDeleted != numAddedValues) {
				throw new MprcException("Could not delete all the elements from the temporary table");
			}
		}

		jobStarter.endJob(bulkLoadJob);
	}

	public Session getSession() {
		return sessionProvider.getSession();
	}
}
