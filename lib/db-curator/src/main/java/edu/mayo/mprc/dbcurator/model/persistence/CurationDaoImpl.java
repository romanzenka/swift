package edu.mayo.mprc.dbcurator.model.persistence;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.DatabasePlaceholder;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.FastaSource;
import edu.mayo.mprc.dbcurator.model.HeaderTransform;
import edu.mayo.mprc.dbcurator.model.SourceDatabaseArchive;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import org.joda.time.DateTime;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that handles data access related to Curation instances.  Others should use CuratorPersistenceManager.  It should
 * be noted that all objects returned from this detached and are not tied to the persistent store.
 * <p/>
 * A singleton accessed by the get() method
 */
public final class CurationDaoImpl extends DaoBase implements CurationDao {
	private List<Curation> allCurationList = null;
	private Change legacyCurationChange = null;
	private static final String MODEL = "edu/mayo/mprc/dbcurator/model/";
	private static final String STEPS = MODEL + "curationsteps/";

	public CurationDaoImpl() {
	}

	public CurationDaoImpl(final DatabasePlaceholder databasePlaceholder) {
		super(databasePlaceholder);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		return Arrays.asList(
				"edu/mayo/mprc/database/Change.hbm.xml",
				STEPS + "CurationStep.hbm.xml",
				STEPS + "DataSource.hbm.xml",
				STEPS + "HeaderTransform.hbm.xml",
				MODEL + "Curation.hbm.xml",
				MODEL + "SourceDatabaseArchive.hbm.xml"
		);
	}

	@Override
	public Curation getCuration(final int curationID) {
		final Session session = getSession();
		Curation curation = null;
		try {
			curation = (Curation) session.get(Curation.class, curationID);
		} catch (Exception t) {
			throw new MprcException("Could not obtain curation ", t);
		}
		return curation;
	}

	@Override
	public Curation getCurationByShortName(final String uniqueName) {
		final List<Curation> curationsByShortname = getCurationsByShortname(uniqueName);
		if(curationsByShortname!=null && !curationsByShortname.isEmpty()) {
			return curationsByShortname.get(0);
		}
		return null;
	}

	@Override
	public Curation getLegacyCuration(final String uniqueName) {
		final Curation result = getCurationByShortName(uniqueName);
		if (result != null) {
			return result;
		}
		// We failed. Let us look at all the deleted ones.
		try {
			final Criteria criteria = getSession().createCriteria(Curation.class)
					.add(Restrictions.isNotNull(DELETION_FIELD))
					.add(Restrictions.eq("shortName", uniqueName))
					.createAlias("deletion", "d") // So we can order by association
					.addOrder(Order.desc("d.date"))
					.setMaxResults(1);

			return (Curation) criteria.uniqueResult();
		} catch (Exception e) {
			throw new MprcException("Cannot get a curation named [" + uniqueName + "]", e);
		}
	}

	/**
	 * try to get the unique name out since it might contain ${DBPath:uniquename} decorations. If it does not,
	 * name is returned verbatim.
	 *
	 * @param possibleUniquename
	 * @return
	 */
	static String extractShortName(final String possibleUniquename) {
		final Matcher uniquenameMatch = Pattern.compile("(?:\\$\\{)(?:DBPath:|DB:)([^}]*)(?:})").matcher(possibleUniquename);
		if (uniquenameMatch.matches()) {
			return uniquenameMatch.group(1); //extract the uniquename from given one.  This should always match I think...
		} else {
			return possibleUniquename;
		}
	}

	/**
	 * Try to get the short name (we could not obtain unique name). A short name occurs either in the ${DB:???_LATEST}
	 * format, or if we are directly given a FASTA file name, it is the part before the date part in 20090101A format.
	 * If both assumptions fail, the name is returned verbatim.
	 *
	 * @param name Name of the database.
	 * @return Extracted short name.
	 */
	static String extractShortname(final String name) {
		final Matcher latestMatch = Pattern.compile("(?:\\$\\{)(?:DBPath:|DB:)(.*)_LATEST(?:\\})").matcher(name);
		if (latestMatch.matches()) {
			return latestMatch.group(1); //we have a _LATEST that we can extract shortname from
		}

		final Matcher uniquenameMatch = Pattern.compile("^(.*)\\d{8}\\D\\.(fasta|FASTA)$").matcher(name);
		if (uniquenameMatch.matches()) {
			return uniquenameMatch.group(1); //we have a uniquename we can extract a shortname from
		}

		//we presumably already have a shortname
		return name;
	}

	@Override
	public Curation findCuration(String name) {
		name = name.trim();
		//try to see if we have a match based on short name
		Curation match = getCurationByShortName(extractShortName(name));
		if (match != null) {
			return match;
		}

		// if there wasn't a match based on unqiue name then we are apprently working on a shortname in which case
		// we will see if the request is for the latest and if so will give the latest of a particular shortname.
		// So we will try to see if there are any matches on the shortname and find the most recently run curation with that shortname.
		final List<Curation> allMatches = getCurationsByShortname(extractShortname(name));

		if (allMatches.size() == 0) {
			match = null;
		} else if (allMatches.size() == 1) {
			match = allMatches.get(0);
			if (match.getCurationFile() == null) {
				match = null;
			}
		} else {
			//sort the list based on the run date of the curation descending
			Collections.sort(allMatches, new Comparator<Curation>() {
				public int compare(final Curation o1, final Curation o2) {
					if (o1.getRunDate() == null && o2.getRunDate() == null) {
						return 0;
					}
					if (o1.getRunDate() == null) {
						return 1;
					}
					if (o2.getRunDate() == null) {
						return -1;
					}
					return -o1.getRunDate().compareTo(o2.getRunDate());
				}
			});
			for (final Curation curation : allMatches) {
				if (curation.getCurationFile() != null) {
					match = curation;
					break;
				}
			}
		}

		if (match == null) {
			throw new MprcException("Could not find a Curation for the given token: " + name);
		}
		return match;
	}

	public List<Curation> getCurationsByShortname(final String shortname) {
		return getCurationsByShortname(shortname, false);
	}

	public List<Curation> getCurationsByShortname(final String shortname, final boolean ignoreCase) {
		final List<Curation> returnList = new ArrayList<Curation>();
		List genericResults = null;
		try {
			final Criteria criteria = allCriteria(Curation.class);
			if (ignoreCase) {
				criteria.add(Restrictions.eq("shortName", shortname).ignoreCase());
			} else {
				criteria.add(Restrictions.eq("shortName", shortname));
			}
			genericResults = criteria.list();
			if (genericResults != null) {
				for (final Object o : genericResults) {
					returnList.add((Curation) o);
				}
			}
		} catch (Exception t) {
			throw new MprcException("Could not obtain list of curations by short name " + shortname + " " + (ignoreCase ? "ignoring case" : "not ignoring case"), t);
		}
		return returnList;
	}

	@Override
	public List<Curation> getAllCurations() {
		return allCurations();
	}

	/**
	 * @return List of all curations from cache, or (if their number changed) from database.
	 */
	private synchronized List<Curation> allCurations() {
		if (this.allCurationList == null) {
			allCurationList = getMatchingCurationsFromDb(null, null, null);
			return allCurationList;
		} else {
			Long count = null;
			try {
				count = (Long) getSession().createQuery("select count(c) from Curation c where c.deletion is null").uniqueResult();
			} catch (Exception t) {
				throw new MprcException("Cannot obtain count of all curations", t);
			}

			if (count != allCurationList.size()) {
				allCurationList = getMatchingCurationsFromDb(null, null, null);
			}

			return allCurationList;
		}
	}

	private List<Curation> getMatchingCurationsFromDb(final Curation templateCuration, final Date earliestRunDate, final Date latestRunDate) {
		final List<Curation> returnList = new ArrayList<Curation>();
		List genericResults = null;

		try {
			final Criteria criteria = allCriteria(Curation.class);
			if (templateCuration != null) {
				criteria.add(Example.create(templateCuration));
			}
			if (earliestRunDate != null) {
				criteria.add(Expression.ge("runDate", earliestRunDate));
			}
			if (latestRunDate != null) {
				criteria.add(Expression.le("runDate", latestRunDate));
			}
			genericResults = criteria.list();
		} catch (Exception t) {
			throw new MprcException("Cannot get matching curations for database " +
					(templateCuration != null ? templateCuration.getShortName() : "") + " and dates between " + earliestRunDate + " and " + latestRunDate, t);
		}
		if (genericResults != null) {
			for (final Object o : genericResults) {
				returnList.add((Curation) o);
			}
		}
		return returnList;
	}

	@Override
	public SourceDatabaseArchive findSourceDatabaseInExistence(final String url, final DateTime fileCreationDate) {
		List<SourceDatabaseArchive> archiveList = null;

		final Session session = getSession();
		try {
			final Criteria criteria = session.createCriteria(SourceDatabaseArchive.class);
			criteria.add(Restrictions.eq("sourceURL", url));
			// serverDate has to match with one second precision - never test timestamp for equality
			criteria.add(Restrictions.ge("serverDate", fileCreationDate.minusSeconds(1)));
			criteria.add(Restrictions.lt("serverDate", fileCreationDate.plusSeconds(1)));
			archiveList = (List<SourceDatabaseArchive>) criteria.list();
		} catch (Exception t) {
			throw new MprcException("Cannot find source database for url: " + url + " and creation date " + fileCreationDate, t);
		}

		if (archiveList != null && archiveList.size() > 0) {
			for (final Object o : archiveList) {
				final SourceDatabaseArchive archive = (SourceDatabaseArchive) o;
				if (archive.getArchive() != null && archive.getArchive().exists()) {
					return archive;
				}
			}
		}
		return null;
	}

	public List<FastaSource> getCommonSources() {
		try {
			return getSession().createQuery("from FastaSource ds where ds.common = true").list();
		} catch (Exception t) {
			throw new MprcException("Cannot obtain a list of FASTA database sources", t);
		}
	}

	private Criterion getHeaderTransformEqualityCriteria(final HeaderTransform transform) {
		return Restrictions.eq("name", transform.getName());
	}

	@Override
	public void addHeaderTransform(final HeaderTransform sprotTrans) {
		this.save(sprotTrans, getHeaderTransformEqualityCriteria(sprotTrans), true);
	}

	public List<HeaderTransform> getCommonHeaderTransforms() {
		try {
			return getSession().createQuery("from HeaderTransform ht where ht.common = true").list();
		} catch (Exception t) {
			throw new MprcException("Cannot obtain a list of common database header transformations", t);
		}
	}

	@Override
	public synchronized Curation addLegacyCuration(final String legacyName) {
		if (legacyCurationChange == null) {
			legacyCurationChange = new Change("Adding legacy databases", new DateTime());
		}

		Curation curation = new Curation();
		curation.setShortName(legacyName);
		curation.setNotes("Legacy database");
		curation.setTitle("Legacy database " + legacyName);
		curation.setOwnerEmail("mprctest@mayo.edu");
		curation.setCurationFile(new File(legacyName + ".fasta"));
		curation = save(curation, legacyCurationChange, getCurationEqualityCriteria(curation), false);
		if (allCurationList != null) {
			allCurationList.add(curation);
		}
		return curation;
	}

	private Criterion getFastaSourceEqualityCriteria(final FastaSource source) {
		return Restrictions.eq("name", source.getName());
	}

	@Override
	public void addFastaSource(final FastaSource source) {
		save(source, getFastaSourceEqualityCriteria(source), true);
	}

	@Override
	public void flush() {
		getSession().flush();
	}

	public void addCuration(final Curation toSave) {
		try {
			save(toSave, new Change("Adding database " + toSave.getShortName(), new DateTime()), getCurationEqualityCriteria(toSave), true);
		} catch (Exception t) {
			throw new MprcException("Could not save an object to hibernate.", t);
		}
	}

	@Override
	public void save(final SourceDatabaseArchive archive) {
		try {
			getSession().saveOrUpdate(archive);
		} catch (Exception t) {
			throw new MprcException("Could not save source database archive", t);
		}
	}

	@Override
	public void deleteCuration(final Curation curation, final Change change) {
		delete(curation, change);
	}

	private Criterion getCurationEqualityCriteria(final Curation curation) {
		return Restrictions.eq("shortName", curation.getShortName());
	}

	public void delete(final Object o) {
		try {
			getSession().delete(o);
		} catch (Exception t) {
			throw new MprcException("Could not delete object from database.", t);
		}
	}

	public FastaSource getDataSourceByName(final String name) {
		List<FastaSource> matches = null;
		try {
			matches = getSession()
					.createQuery("from FastaSource ds where ds.name = :name")
					.setParameter("name", name)
					.list();
		} catch (Exception t) {
			throw new MprcException("Cannot get data sources by name " + name, t);
		}

		return matches == null || matches.isEmpty() ? null : matches.get(0);
	}

	public FastaSource getDataSourceByUrl(final String url) {
		List<FastaSource> matches = null;
		try {
			matches = getSession()
					.createQuery("from FastaSource ds where ds.url = :url")
					.setParameter("url", url)
					.list();
		} catch (Exception t) {
			throw new MprcException("Cannot get data sources by url " + url, t);
		}

		return matches == null || matches.isEmpty() ? null : matches.get(0);
	}

	public HeaderTransform getHeaderTransformByName(final String name) {
		List<HeaderTransform> matches = null;
		try {
			matches = getSession()
					.createQuery("from HeaderTransform ht where ht.name = :name")
					.setParameter("name", name)
					.list();
		} catch (Exception t) {
			throw new MprcException("Cannot get database header transformation named [" + name + "]", t);
		}
		return (matches == null || matches.isEmpty() ? null : matches.get(0));
	}

	public HeaderTransform getHeaderTransformByUrl(final String forUrl) {
		List<HeaderTransform> matches = null;
		try {
			matches = getSession()
					.createQuery("select ds.transform from FastaSource ds where ds.url = :forUrl")
					.setParameter("forUrl", forUrl)
					.list();
		} catch (Exception t) {
			throw new MprcException("Cannot get database header transformation by url " + forUrl, t);
		}
		return (matches == null || matches.isEmpty() ? null : matches.get(0));
	}
}
