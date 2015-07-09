package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.database.SessionProvider;
import org.hibernate.*;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.jdbc.Work;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

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
	protected final SessionProvider sessionProvider;

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

	/**
	 * Get equality string. The references to the tables are simplified to &lt;s>. and &lt;t>.
	 * These will get replaced with the original table name (s) and the temp table name (t)
	 */
	public abstract String getEqualityString();

	/**
	 * @param value value to wrap
	 * @return wrapped value
	 */
	public abstract Object wrapForTempTable(T value, TempKey key);

	/**
	 * Comma separated list of columns to transfer from the temp table.
	 */
	public abstract String getColumnsFromTemp();

	/**
	 * Comma separated list of columns to transfer to the actual table.
	 */
	public abstract String getColumnsToTarget();

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
		final String equalityString = getEqualityString()
				.replaceAll(Pattern.quote("<t>"), tempTableName)
				.replaceAll(Pattern.quote("<s>"), "s");

		final String subSelect =
				MessageFormat.format("SELECT s.{1} from {2} as s where {3} and {0}.job = :job",
						sessionProvider.qualifyTableName(tempTableName), tableId, sessionProvider.qualifyTableName(table), equalityString);

		// This is absolutely hideous.
		// We ask hibernate dialect to figure out how to do the limit operation.
		// Hibernate will return a string with ? in place of the number 1 (we hope)
		// We replace that ? with the number 1
		// This is all done because MS SQL does not support LIMIT operation, instead it needs TOP
		final String limitedSubSelect =
				sessionProvider.getDialect().getLimitString(subSelect, 0, 1).replace('?', '1');

		final SQLQuery sqlQuery = getSession().createSQLQuery(
				MessageFormat.format(
						"UPDATE {0} SET {0}.new_id = ({1}) where {0}.job = :job",
						sessionProvider.qualifyTableName(tempTableName), limitedSubSelect));
		sqlQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		final int update1 = sqlQuery.executeUpdate();
		return update1;
	}

	protected int getLastId() {
		final String table = getTableName();
		final String tableId = getTableIdColumn();

		try {
			final Integer lastId = (Integer) getSession()
					.createSQLQuery("select max(" + tableId + ") from " + sessionProvider.qualifyTableName(table))
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
		final String columnsToTranferFrom = getColumnsFromTemp();
		final String columnsToTranferTo = getColumnsToTarget();
		try {
			identityOn(table);
			final Query query = getSession()
					.createSQLQuery(
							MessageFormat.format(
									"INSERT INTO {0} ({1}, {2}) select data_order+{3,number,#}, {4} from {5} where job = :job and new_id is null",
									sessionProvider.qualifyTableName(table), tableId, columnsToTranferFrom, lastId, columnsToTranferTo,
									sessionProvider.qualifyTableName(tempTableName)))
					.setParameter("job", bulkLoadJob.getId());
			query.executeUpdate();
			identityOff(table);
		} catch (Exception e) {
			throw new MprcException("Cannot insert new data to " + table, e);
		}

		try {
			final Query query2 = getSession()
					.createSQLQuery(
							MessageFormat.format(
									"UPDATE {0} SET new_id = data_order+{1,number,#} where job = :job and new_id is null",
									sessionProvider.qualifyTableName(tempTableName), lastId))
					.setParameter("job", bulkLoadJob.getId());
			query2.executeUpdate();
		} catch (Exception e) {
			throw new MprcException("Cannot update existing data in " + tempTableName, e);
		}
	}

	protected void identityOff(final String table) {
		if (sessionProvider.getDialect() instanceof SQLServerDialect) {
			getSession().doWork(new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					Statement statement = connection.createStatement();
					statement.execute("SET IDENTITY_INSERT " + sessionProvider.qualifyTableName(table) + " OFF;");
				}
			});
		}
	}

	protected void identityOn(final String table) {
		if (sessionProvider.getDialect() instanceof SQLServerDialect) {
			getSession().doWork(new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					Statement statement = connection.createStatement();
					statement.execute("SET IDENTITY_INSERT " + sessionProvider.qualifyTableName(table) + " ON;");
				}
			});
		}
	}

	protected void loadNewIdsBack(final Collection<? extends T> values, final BulkLoadJob bulkLoadJob) {
		final String tempTableName = getTempTableName();
		final Query query = getSession().createSQLQuery(
				MessageFormat.format(
						"SELECT new_id FROM {0} t WHERE t.job = :job ORDER BY t.data_order",
						sessionProvider.qualifyTableName(tempTableName)));

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
						"DELETE FROM {0} WHERE job=:job", sessionProvider.qualifyTableName(tempTableName)));
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
