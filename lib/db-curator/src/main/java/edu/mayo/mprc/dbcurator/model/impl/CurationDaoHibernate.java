package edu.mayo.mprc.dbcurator.model.impl;

import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.dbcurator.model.*;
import edu.mayo.mprc.dbcurator.model.curationsteps.MakeDecoyStep;
import edu.mayo.mprc.dbcurator.model.curationsteps.NewDatabaseInclusion;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.usertype.UserType;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
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
@Repository("curationDao")
public final class CurationDaoHibernate extends DaoBase implements CurationDao {
	private static final Logger LOGGER = Logger.getLogger(CurationDaoHibernate.class);

	private List<Curation> allCurationList = null;

	private CurationContext context;

	private static final String MODEL = "edu/mayo/mprc/dbcurator/model/";
	private static final String STEPS = MODEL + "curationsteps/";

	// Needed for initialization of the database
	private static final String TEST_URL = "classpath:/edu/mayo/mprc/dbcurator/ShortTest.fasta.gz";

	public CurationDaoHibernate() {
	}

	public CurationDaoHibernate(final Database database) {
		super(database);
	}

	public CurationDaoHibernate(final CurationContext context) {
		this.context = context;
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				STEPS + "DataSource.hbm.xml",
				STEPS + "HeaderTransform.hbm.xml",
				MODEL + "Curation.hbm.xml",
				MODEL + "SourceDatabaseArchive.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}

    @Override
    public Map<String, UserType> getUserTypes() {
        HashMap<String, UserType> res = Maps.newHashMap();
        res.put("JsonCurationSteps", new CurationStepListType());
        return res;
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
		if (curationsByShortname != null && !curationsByShortname.isEmpty()) {
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

		if (allMatches.isEmpty()) {
			match = null;
		} else if (allMatches.size() == 1) {
			match = allMatches.get(0);
			if (match.getCurationFile() == null) {
				match = null;
			}
		} else {
			//sort the list based on the run date of the curation descending
			Collections.sort(allMatches, new Comparator<Curation>() {
				@Override
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

	@Override
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
		if (allCurationList == null) {
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
				criteria.add(Restrictions.ge("runDate", earliestRunDate));
			}
			if (latestRunDate != null) {
				criteria.add(Restrictions.le("runDate", latestRunDate));
			}
			genericResults = criteria.list();
		} catch (Exception t) {
			throw new MprcException("Cannot get matching curations for database " +
					(templateCuration != null ? templateCuration.getShortName() : "") + " and dates between " + earliestRunDate + " and " + latestRunDate, t);
		}
		if (genericResults != null) {
			for (final Object o : genericResults) {
				final Curation origCuration = (Curation) o;
				final Curation curationCopy = origCuration.copyFull();
				curationCopy.setId(origCuration.getId());
				returnList.add(curationCopy);
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

		if (archiveList != null && !archiveList.isEmpty()) {
			for (final Object o : archiveList) {
				final SourceDatabaseArchive archive = (SourceDatabaseArchive) o;
				if (archive.getArchive() != null && archive.getArchive().exists()) {
					return archive;
				}
			}
		}
		return null;
	}

	@Override
	public List<FastaSource> getCommonSources() {
		try {
			return getSession().createQuery("from FastaSource ds where ds.common = true").list();
		} catch (Exception t) {
			throw new MprcException("Cannot obtain a list of FASTA database sources", t);
		}
	}

	public void addHeaderTransform(final HeaderTransform sprotTrans) {
		save(sprotTrans, true);
	}

	@Override
	public List<HeaderTransform> getCommonHeaderTransforms() {
		try {
			return getSession().createQuery("from HeaderTransform ht where ht.common = true").list();
		} catch (Exception t) {
			throw new MprcException("Cannot obtain a list of common database header transformations", t);
		}
	}

	public void addFastaSource(final FastaSource source) {
		save(source, true);
	}

	@Override
	public void flush() {
		getSession().flush();
	}

	@Override
	public void addCuration(final Curation toSave) {
		try {
			save(toSave, new Change("Adding database " + toSave.getShortName(), new DateTime()), true);
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

	public void delete(final Object o) {
		try {
			getSession().delete(o);
		} catch (Exception t) {
			throw new MprcException("Could not delete object from database.", t);
		}
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public String check() {
		LOGGER.info("Checking database curations");
		if (countAll(Curation.class) == 0) {
			return "There needs to be at least one FASTA database defined";
		}
		if (rowCount(HeaderTransform.class) == 0) {
			return "There needs to be at least one FASTA header transformation preset available";
		}
		return null;
	}

	@Override
	public void install(Map<String, String> params) {
		LOGGER.info("Installing database curator data");

		final boolean test = params.containsKey("test");

		HeaderTransform sprotTrans = null;
		HeaderTransform ipiTrans = null;
		HeaderTransform ncbiTrans = null;
		if (rowCount(HeaderTransform.class) == 0) {
			LOGGER.info("Filling FASTA header transformation steps table");
			sprotTrans = new HeaderTransform().setName("SwissProt General").setGroupString("^>([^|]*)\\|([^|]*)\\|(.*)$").setSubstitutionPattern(">$3 ($1) ($2)").setCommon(true);
			addHeaderTransform(sprotTrans);
			ipiTrans = new HeaderTransform().setName("IPI General").setGroupString("^>IPI:([^.^|^\\s]+)\\S* (Tax_Id=\\S+)?(?:Gene_Symbol=\\S+)?(.*)").setSubstitutionPattern(">$1 $3 $2").setCommon(true);
			addHeaderTransform(ipiTrans);
			ncbiTrans = new HeaderTransform().setName("NCBI General").setGroupString("^>(gi\\|([^| ]+)[^\\s]*)\\s([^\\x01\\r\\n]+)(.*)$").setSubstitutionPattern(">gi$2 $3 ;$1 $4").setCommon(true);
			addHeaderTransform(ncbiTrans);
		} else {
			sprotTrans = getHeaderTransformByName("SwissProt General");
			ipiTrans = getHeaderTransformByName("IPI General");
			ncbiTrans = getHeaderTransformByName("NCBI General");
		}

		if (rowCount(FastaSource.class) == 0) {
			LOGGER.info("Filling FASTA sources");
			if (sprotTrans != null) {
				addFastaSource(new FastaSource()
						.setName("Sprot_complete")
						.setUrl("ftp://ftp.expasy.org/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz")
						.setCommon(true)
						.setTransform(sprotTrans));
			}

			if (ipiTrans != null) {
				addFastaSource(new FastaSource()
						.setName("IPI_Human")
						.setUrl("ftp://ftp.ebi.ac.uk/pub/databases/IPI/current/ipi.HUMAN.fasta.gz")
						.setCommon(true)
						.setTransform(ipiTrans));

				addFastaSource(new FastaSource()
						.setName("IPI_Mouse")
						.setUrl("ftp://ftp.ebi.ac.uk/pub/databases/IPI/current/ipi.MOUSE.fasta.gz")
						.setCommon(true)
						.setTransform(ipiTrans));
			}

			if (ncbiTrans != null) {
				addFastaSource(new FastaSource()
						.setName("NCBInr")
						.setUrl("ftp://ftp.ncbi.nih.gov/blast/db/FASTA/nr.gz")
						.setCommon(true)
						.setTransform(ncbiTrans));
			}

			addFastaSource(new FastaSource()
					.setName("ShortTest")
					.setUrl("classpath:/edu/mayo/mprc/dbcurator/ShortTest.fasta.gz")
					.setCommon(true)
					.setTransform(null));
		}
		flush();

		if (countAll(Curation.class) == 0) {
			final Set<Curation> toExecute = new HashSet<Curation>();

			if (TEST_URL != null) {
				//if the database doesn't have a Sprot database then lets create one.
				if (getCurationsByShortname("ShortTest").isEmpty()) {
					LOGGER.debug("Creating Curation 'ShortTest' from " + TEST_URL);
					final Curation shortTest = new Curation();
					shortTest.setShortName("ShortTest");

					shortTest.setTitle("Built-in");

					final NewDatabaseInclusion step1 = new NewDatabaseInclusion();
					step1.setUrl(TEST_URL);

					shortTest.addStep(step1, /*position*/-1);

					final MakeDecoyStep step3 = new MakeDecoyStep();
					step3.setManipulatorType(MakeDecoyStep.REVERSAL_MANIPULATOR);
					step3.setOverwriteMode(/*overwrite?*/false);
					shortTest.addStep(step3, /*position*/-1);

					toExecute.add(shortTest);
				}

			} else {
				LOGGER.debug("Could not find a URL to apply to 'ShortTest'");
			}

			//execute the ones we decided to execute
			final File localTempFolder = FileUtilities.createTempFolder();
			try {
				ensureFoldersExists();
				for (final Curation curation : toExecute) {
					LOGGER.info("Executing curation: " + curation.getShortName());
					final CurationExecutor executor = new CurationExecutor(curation, true, this, context.getFastaFolder(), localTempFolder, context.getFastaArchiveFolder());
					// Execute the curation within our thread
					executor.executeCuration();
					final CurationStatus status = executor.getStatusObject();

					for (final String message : status.getMessages()) {
						LOGGER.debug(message);
					}

					//if we had a failure then let's figure out why
					if (status.getFailedStepValidations() != null && !status.getFailedStepValidations().isEmpty()) {
						LOGGER.error("Could not execute curation: " + curation.getShortName()
								+ "\nStep validation failed:\n"
								+ CurationExecutor.failedValidationsToString(status.getFailedStepValidations()));
					}
				}

			} finally {
				FileUtilities.cleanupTempFile(localTempFolder);
			}

			LOGGER.info("Done seeding Curation database tables.");
		}
	}

	/**
	 * The curator needs these folders to exist. Check they are there, if not, create them.
	 */
	private void ensureFoldersExists() {
		FileUtilities.ensureFolderExists(context.getFastaFolder());
		FileUtilities.ensureFolderExists(context.getFastaArchiveFolder());
		FileUtilities.ensureFolderExists(context.getFastaUploadFolder());
	}

	public CurationContext getContext() {
		return context;
	}

	@Resource(name = "curationContext")
	public void setContext(final CurationContext context) {
		this.context = context;
	}
}
