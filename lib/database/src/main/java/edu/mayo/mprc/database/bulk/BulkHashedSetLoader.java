package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.database.PersistableHashedSetBase;
import edu.mayo.mprc.database.SessionProvider;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

import java.text.MessageFormat;

/**
 * @author Roman Zenka
 */
public abstract class BulkHashedSetLoader<T extends PersistableHashedSetBase<? extends PersistableBase>> extends BulkLoader<T> {
	public static final String TEMP_TABLE = "temp_hashed_set";
	public static final String TEMP_MEMBERS = "temp_hashed_set_member";

	public BulkHashedSetLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public final String getTempTableName() {
		return TEMP_TABLE;
	}

	public abstract String getMemberTableName();

	public String getMemberTableKey() {
		// By default the member table links using columns of identical name as the referenced table keys
		return getTableIdColumn();
	}

	public abstract String getMemberTableValue();

	@Override
	public String getEqualityString() {
		return MessageFormat.format("" +
				"<t>.hash = <s>.hash " +
				"and not exists (" +

				"select * from {0} as tm " +
				"left join {1} as sm " +
				"on tm.value = sm.{2} " +
				"   where " +
				"tm.job = <t>.job " +
				"and tm.data_order = <t>.data_order " +
				"and <s>.{3} = sm.{4} " +
				"and sm.{2} is null " +

				"union all " +

				"select * from {1} as sm " +
				"left join {0} as tm " +
				"on tm.value = sm.{2} " +
				"   where " +
				"tm.job = <t>.job " +
				"and tm.data_order = <t>.data_order " +
				"and <s>.{3} = sm.{4} " +
				"and tm.value is null " +
				")",
				sessionProvider.qualifyTableName(TEMP_MEMBERS),
				sessionProvider.qualifyTableName(getMemberTableName()),
				getMemberTableValue(),
				getTableIdColumn(),
				getMemberTableKey());
	}

	@Override
	public Object wrapForTempTable(final T value, final TempKey key) {
		final TempHashedSet result = new TempHashedSet();
		result.setTempKey(key);
		value.calculateHash();
		// Wrap and save all the members. We need to store these for extra equality checks.
		for (final PersistableBase member : value.getList()) {
			if (member.getId() == null) {
				throw new MprcException("The hashed set members have to be all saved in the database (ids associated) before bulk loading");
			}
			getSession().save(new TempHashedSetMember(key, member.getId()));
		}
		result.setHash(value.getHash());
		return result;
	}


	@Override
	public String getColumnsToTransfer() {
		return "hash";
	}

	/**
	 * We need to clean not only the temporary table itself, but also the temporary
	 * table of set members.
	 */
	@Override
	protected void deleteTemp(final BulkLoadJob bulkLoadJob, final int numAddedValues) {
		final SQLQuery deleteQuery = getSession().createSQLQuery(
				MessageFormat.format(
						"DELETE FROM {0} WHERE job=:job", sessionProvider.qualifyTableName(TEMP_MEMBERS)));
		deleteQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		deleteQuery.executeUpdate();

		super.deleteTemp(bulkLoadJob, numAddedValues);
	}

	/**
	 * We need to insert not only into the table itself, but also into the member table.
	 */
	@Override
	protected void insertMissing(final BulkLoadJob bulkLoadJob, final int lastId) {
		final String table = getTableName();
		final String tableId = getTableIdColumn();
		final String tempTableName = getTempTableName();
		final String columnsToTranfer = getColumnsToTransfer();
		try {
			identityOn(table);
			final Query query = getSession()
					.createSQLQuery(
							MessageFormat.format(
									"INSERT INTO {0} ({1}, {2}) select data_order+{3,number,#}, {4} from {5} where job = :job and new_id is null",
									sessionProvider.qualifyTableName(table), tableId, columnsToTranfer, lastId, columnsToTranfer, sessionProvider.qualifyTableName(tempTableName)))
					.setParameter("job", bulkLoadJob.getId());
			query.executeUpdate();
			identityOff(table);
		} catch (Exception e) {
			throw new MprcException("Cannot insert new data to " + table, e);
		}

		try {
			final Query query = getSession()
					.createSQLQuery(
							MessageFormat.format(
									"INSERT INTO {0} ({1}, {2}) select m.data_order+{3,number,#}, m.value from {4} as m inner join {5} as t on m.job=t.job and m.data_order=t.data_order where t.job = :job and t.new_id is null",
									sessionProvider.qualifyTableName(getMemberTableName()), getMemberTableKey(), getMemberTableValue(), lastId,
									sessionProvider.qualifyTableName(TEMP_MEMBERS), sessionProvider.qualifyTableName(TEMP_TABLE)))
					.setParameter("job", bulkLoadJob.getId());
			query.executeUpdate();
		} catch (Exception e) {
			throw new MprcException("Cannot insert new data to member table " + getMemberTableName(), e);
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
}
