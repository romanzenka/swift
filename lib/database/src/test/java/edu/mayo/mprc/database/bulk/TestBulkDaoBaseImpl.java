package edu.mayo.mprc.database.bulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class TestBulkDaoBaseImpl extends BulkDaoBase {
	public TestBulkDaoBaseImpl() {
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				"edu/mayo/mprc/database/TestDate.hbm.xml",
				"edu/mayo/mprc/database/TestDouble.hbm.xml",
				"edu/mayo/mprc/database/TestList.hbm.xml",
				"edu/mayo/mprc/database/TestSet.hbm.xml",
				"edu/mayo/mprc/database/TestSetMember.hbm.xml"));
		list.addAll(super.getHibernateMappings());
		return list;
	}

}
