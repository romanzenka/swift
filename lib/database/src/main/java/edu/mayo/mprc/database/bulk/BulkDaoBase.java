package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A base class similar to {@link DaoBase} that also supports bulk loading operations.
 *
 * @author Roman Zenka
 */
public abstract class BulkDaoBase extends DaoBase implements BulkLoadJobStarter {
	protected BulkDaoBase() {
	}

	protected BulkDaoBase(final Database database) {
		super(database);
	}

	/**
	 * Provides a list of all hibernate mapping files (.hbm.xml) that are needed for this Dao to function.
	 *
	 * @return List of hibernate mapping files to be used.
	 */
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				"edu/mayo/mprc/database/bulk/BulkLoadJob.hbm.xml",
				"edu/mayo/mprc/database/bulk/TempHashedSet.hbm.xml",
				"edu/mayo/mprc/database/bulk/TempHashedSetMember.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	@Override
	public BulkLoadJob startNewJob() {
		final BulkLoadJob job = new BulkLoadJob();
		getSession().save(job);
		return job;
	}

	@Override
	public void endJob(final BulkLoadJob job) {
		getSession().delete(job);
	}
}
