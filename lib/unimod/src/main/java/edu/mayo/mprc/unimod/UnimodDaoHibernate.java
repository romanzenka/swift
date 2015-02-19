package edu.mayo.mprc.unimod;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.apache.log4j.Logger;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.*;

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

	public UnimodDaoHibernate(final Database database) {
		super(database);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		List<String> list = new ArrayList<String>(Arrays.asList(
				HBM_DIR + "Mod.hbm.xml",
				HBM_DIR + "ModSet.hbm.xml",
				HBM_DIR + "ModSpecificity.hbm.xml"));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	@Override
	public Unimod load() {
		final Session session = getSession();
		try {
			final List<Mod> list = listAndCast(
					allCriteria(Mod.class)
							.setFetchMode("modSpecificities", FetchMode.JOIN)
							.setFetchMode("altNames", FetchMode.JOIN)
							.setReadOnly(true));
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
			final List<Mod> list = listAndCast(allCriteria(Mod.class));
			final UnimodUpgrade upgrade = new UnimodUpgrade();
			upgrade.upgrade(list, unimod, request, getSession());
			return upgrade;
		} catch (Exception t) {
			throw new MprcException("Database upgrade " + (request != null ? request : "") + " failed", t);
		}
	}

	@Override
	public String check() {
		LOGGER.info("Checking unimod DAO");
		if (countAll(Mod.class) == 0) {
			return "No unimod modifications defined";
		}
		return null;
	}

	@Override
	public void install(Map<String, String> params) {
		if (countAll(Mod.class) == 0) {
			LOGGER.info("Installing unimod DAO");
			final Change change = new Change("Installing initial unimod modifications", new DateTime());
			LOGGER.info(change.getReason());
			// Load short version of unimod
			final Unimod unimod = unimodFromResource("classpath:edu/mayo/mprc/unimod/unimod_short.xml");

			final UnimodUpgrade upgrade = upgrade(unimod, change);
			LOGGER.debug("Unimod install results: " + upgrade.toString());
		}
	}

	@Override
	public Unimod getDefaultUnimod() {
		return unimodFromResource("classpath:edu/mayo/mprc/unimod/unimod.xml");
	}

	private Unimod unimodFromResource(String sourcePath) {
		final Unimod unimod = new Unimod();
		try {
			unimod.parseUnimodXML(ResourceUtilities.getStream(sourcePath, Unimod.class));
		} catch (Exception t) {
			throw new MprcException(String.format("Unable to parse unimod set from [%s]", sourcePath), t);
		}
		return unimod;
	}
}
