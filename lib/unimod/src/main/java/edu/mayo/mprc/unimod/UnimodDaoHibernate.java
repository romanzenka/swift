package edu.mayo.mprc.unimod;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.DatabasePlaceholder;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides access to the unimod set from the database.
 * Since the set is reasonably small, it can be loaded completely in one go, indexed in memory
 * and processed more efficiently.
 * <p/>
 * The DAO allows you to return current modification set, and to upgrade the set from a given file.
 */
@Repository("unimodDao")
public final class UnimodDaoHibernate extends DaoBase implements UnimodDao {
	private static final Logger LOGGER = Logger.getLogger(UnimodDaoHibernate.class);

	private static final String HBM_DIR = "edu/mayo/mprc/unimod/";

	public UnimodDaoHibernate() {
	}

	public UnimodDaoHibernate(final DatabasePlaceholder databasePlaceholder) {
		super(databasePlaceholder);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		return Arrays.asList(
				HBM_DIR + "Mod.hbm.xml",
				HBM_DIR + "ModSet.hbm.xml",
				HBM_DIR + "ModSpecificity.hbm.xml",
				"edu/mayo/mprc/database/Change.hbm.xml");
	}

	@Override
	public Unimod load() {
		final Session session = getSession();
		try {
			final List<Mod> list = (List<Mod>) allCriteria(Mod.class)
					.setReadOnly(true)
					.list();
			final Unimod unimod = new Unimod();
			for (final Mod mod : list) {
				unimod.add(mod);
				session.evict(mod);
			}
			return unimod;
		} catch (Exception t) {
			throw new MprcException("Cannot load unimod data from database", t);
		}
	}

	@Override
	public UnimodUpgrade upgrade(final Unimod unimod, final Change request) {
		try {
			final List<Mod> list = (List<Mod>) allCriteria(Mod.class).list();
			final UnimodUpgrade upgrade = new UnimodUpgrade();
			upgrade.upgrade(list, unimod, request, getSession());
			return upgrade;
		} catch (Exception t) {
			throw new MprcException("Database upgrade " + (request != null ? request : "") + " failed", t);
		}
	}

	@Override
	public String check(final Map<String, String> params) {
		if (countAll(Mod.class) == 0) {
			return "No unimod modifications defined";
		}
		return null;
	}

	@Override
	public void initialize(final Map<String, String> params) {
		if (countAll(Mod.class) == 0) {
			final Change change = new Change("Installing initial unimod modifications", new DateTime());
			LOGGER.info(change.getReason());
			final Unimod unimod = getDefaultUnimod();

			final UnimodUpgrade upgrade = upgrade(unimod, change);
			LOGGER.debug("Unimod install results: " + upgrade.toString());
		}
	}

	public Unimod getDefaultUnimod() {
		final Unimod unimod = new Unimod();
		try {
			unimod.parseUnimodXML(ResourceUtilities.getStream("classpath:edu/mayo/mprc/unimod/unimod.xml", Unimod.class));
		} catch (Exception t) {
			throw new MprcException("Unable to parse default unimod set", t);
		}
		return unimod;
	}

}
