package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.database.PersistableHashedSetBase;
import edu.mayo.mprc.database.SessionProvider;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.testng.Assert;

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
				"t.hash = s.hash " +
				"and not exists (" +

				"select * from {0} as tm " +
				"left join {1} as sm " +
				"on tm.value = sm.{2} " +
				"   where " +
				"tm.job = t.job " +
				"and tm.data_order = t.data_order " +
				"and s.{3} = sm.{4} " +
				"and sm.{2} is null " +

				"union all " +

				"select * from {1} as sm " +
				"left join {0} as tm " +
				"on tm.value = sm.{2} " +
				"   where " +
				"tm.job = t.job " +
				"and tm.data_order = t.data_order " +
				"and s.{3} = sm.{4} " +
				"and tm.value is null " +
				")",
				TEMP_MEMBERS,
				getMemberTableName(),
				getMemberTableValue(),
				getTableIdColumn(),
				getMemberTableKey());
	}

	@Override
	public Object wrapForTempTable(final T value, final TempKey key) {
		final TempHashedSet result = new TempHashedSet();
		result.setTempKey(key);
		for (final PersistableBase member : value.getList()) {
			Assert.assertNotNull(member.getId());
			result.getMembers().add(new TempHashedSetMember(key, member.getId()));
		}
		result.calculateHash();
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
						"DELETE FROM {0} WHERE job=:job", TEMP_MEMBERS));
		deleteQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		deleteQuery.executeUpdate();

		super.deleteTemp(bulkLoadJob, numAddedValues);
	}

	/**
	 * We need to insert not only into the table itself, but also of the table with members.
	 */
	@Override
	protected void insertMissing(final BulkLoadJob bulkLoadJob, final int lastId) {
		super.insertMissing(bulkLoadJob, lastId);
		final Query query = getSession()
				.createSQLQuery(
						MessageFormat.format(
								"INSERT INTO {0} ({1}, {2}) select data_order+{3}, value from {4} where job = :job",
								getMemberTableName(), getMemberTableKey(), getMemberTableValue(), lastId, TEMP_MEMBERS))
				.setParameter("job", bulkLoadJob.getId());
		query.executeUpdate();
	}
}
