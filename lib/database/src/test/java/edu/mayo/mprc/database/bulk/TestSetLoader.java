package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.SessionProvider;
import edu.mayo.mprc.database.TestList;

/**
 * @author Roman Zenka
 */
public final class TestSetLoader extends BulkHashedSetLoader<TestList> {
	public TestSetLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
		super(jobStarter, sessionProvider);
	}

	@Override
	public String getTableName() {
		return "test_list";
	}

	@Override
	public String getMemberTableName() {
		return "test_list_members";
	}

	@Override
	public String getMemberTableValue() {
		return "test_set_member_id";
	}
}
