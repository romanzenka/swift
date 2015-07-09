package edu.mayo.mprc.fastadb;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.bulk.BulkDaoBase;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.fasta.FASTAInputStream;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.PercentRangeReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * Hibernate implementation of {@link FastaDbDao}.
 *
 * @author Roman Zenka
 */
@Repository("fastaDbDao")
public final class FastaDbDaoHibernate extends BulkDaoBase implements FastaDbDao {
	public static final int IDS_AT_A_TIME = 1000;
	private static final Logger LOGGER = Logger.getLogger(FastaDbDaoHibernate.class);
	// Progress will be checked each X spectra
	public static final long REPORT_FREQUENCY = 100L;
	private static final String HBM_HOME = "edu/mayo/mprc/fastadb/";

	private CurationDao curationDao;

	public FastaDbDaoHibernate() {
	}

	public FastaDbDaoHibernate(final CurationDao curationDao) {
		this.curationDao = curationDao;
	}

	public FastaDbDaoHibernate(final Database database) {
		super(database);
	}

	@Override
	public ProteinSequence getProteinSequence(final Curation database, final String accessionNumber) {
		Preconditions.checkNotNull(database, "Database has to be specified");
		return (ProteinSequence) getSession()
				.createQuery("select e.sequence from " +
						"ProteinEntry e " +
						"where e.database=:database and e.accessionNumber.accnum=:accessionNumber")
				.setEntity("database", database)
				.setString("accessionNumber", accessionNumber)
				.uniqueResult();
	}

	@Override
	public Map<String, ProteinSequence> getProteinSequences(final Curation database, final Collection<String> accessionNumbers) {
		Preconditions.checkNotNull(database, "Database has to be specified");
		if (accessionNumbers.isEmpty()) {
			return Maps.newHashMap();
		}

		final Set<String> unmappedAccnums = Sets.newHashSetWithExpectedSize(accessionNumbers.size());

		for (final String accNum : accessionNumbers) {
			unmappedAccnums.add(accNum.toUpperCase(Locale.US));
		}

		final Object[] ids = unmappedAccnums.toArray();

		final Map<String, ProteinSequence> completeMap = Maps.newHashMapWithExpectedSize(ids.length);

		for (int i = 0; i < ids.length; i += IDS_AT_A_TIME) {
			final int endIndex = Math.min(ids.length, i + IDS_AT_A_TIME);
			final Query query = getSession()
					.createQuery("select a.accnum, e.sequence from ProteinEntry as e, ProteinAccnum a " +
							"where e.accessionNumber = a and e.database=:database and upper(a.accnum) in (:ids)")
					.setParameter("database", database)
					.setParameterList("ids", Arrays.copyOfRange(ids, i, endIndex));

			final List<Object> objects = listAndCast(query);
			for (final Object o : objects) {
				final Object[] a = (Object[]) o;
				final String accNum = ((String) a[0]).toUpperCase(Locale.US);
				unmappedAccnums.remove(accNum);
				final ProteinSequence sequence = (ProteinSequence) a[1];
				if (sequence == null) {
					throw new MprcException(MessageFormat.format("Could not find description for protein [{0}] in database [{1}]", accNum, database.getShortName()));
				}
				completeMap.put(accNum, sequence);
			}
		}

		if (unmappedAccnums.size() > 0) {
			throw new MprcException("Database " + database.getShortName() + " did not contain " + unmappedAccnums.size() + " accession numbers: " + Joiner.on(", ").join(unmappedAccnums).toString());
		}

		return completeMap;
	}

	@Override
	public String getProteinDescription(final Curation database, final String accessionNumber) {
		try {
			return (String) getSession()
					.createQuery("select e.description.description from ProteinEntry e where e.accessionNumber.accnum = :accessionNumber and e.database=:database")
					.setParameter("accessionNumber", accessionNumber)
					.setParameter("database", database)
					.uniqueResult();
		} catch (final Exception e) {
			throw new MprcException(MessageFormat.format("Could not find description for protein [{0}] in database [{1}]", accessionNumber, database.getShortName()), e);
		}
	}

	@Override
	public ProteinSequence addProteinSequence(final ProteinSequence proteinSequence) {
		if (null == proteinSequence.getId()) {
			return save(proteinSequence, false);
		}
		return proteinSequence;
	}

	@Override
	public ProteinSequence getProteinSequence(final int proteinId) {
		return (ProteinSequence) getSession().get(ProteinSequence.class, proteinId);
	}

	@Override
	public long countDatabaseEntries(final Curation database) {
		return (Long) getSession().createQuery("select count(*) from ProteinEntry p where p.database=:database").setEntity("database", database)
				.uniqueResult();
	}

	/**
	 * This method opens its own stateless session for its duration, so you do not need to call {@link #begin}
	 * or {@link #commit} around this method. This makes the method quite special.
	 * <p/>
	 * If the curation was already previously loaded into the database, the method does nothing.
	 *
	 * @param database Database to load data for.
	 */
	@Override
	public void addFastaDatabase(final Curation database, @Nullable final UserProgressReporter progressReporter) {
		final Query entryCount = getSession().createQuery("select count(*) from ProteinEntry p where p.database=:database").setEntity("database", database);
		if (0L != ((Long) entryCount.uniqueResult()).longValue()) {
			// We have loaded the database already
			return;
		}

		final File fasta = database.getFastaFile().getFile();
		FileUtilities.ensureReadableFile("fasta database", fasta);
		final FASTAInputStream stream = new FASTAInputStream(fasta);
		final PercentDoneReporter percentReporter = new PercentDoneReporter(
				progressReporter,
				MessageFormat.format("Loading [{0}] to database: ", fasta.getAbsolutePath()));

		final PercentRangeReporter parsingRange = new PercentRangeReporter(percentReporter, 0f, 0.5f);
		final PercentRangeReporter loadingRange = new PercentRangeReporter(percentReporter, 0.5f, 1.0f);
		final PercentRangeReporter initialLoad = new PercentRangeReporter(loadingRange, 0f, 0.2f);
		final PercentRangeReporter entryLoad = new PercentRangeReporter(loadingRange, 0.2f, 1.0f);

		try {
			stream.beforeFirst();
			long numSequencesRead = 0L;

			final List<ProteinEntry> entries = new ArrayList<ProteinEntry>(50000);
			final List<ProteinSequence> sequences = new ArrayList<ProteinSequence>(50000);
			// Descriptions can be non-unique
			final Map<String, ProteinDescription> descriptions = Maps.newHashMapWithExpectedSize(50000);
			// Accnums must be unique
			final List<ProteinAccnum> accnums = new ArrayList<ProteinAccnum>(50000);

			while (stream.gotoNextSequence()) {
				numSequencesRead++;
				final String header = stream.getHeader();
				final String sequence = stream.getSequence();
				final int space = header.indexOf(' ');
				final String accessionNumber;
				final String description;
				if (1 <= space) {
					accessionNumber = header.substring(1, space);
					description = header.substring(space + 1).trim();
				} else {
					accessionNumber = header.substring(1);
					description = "";
				}

				final ProteinSequence proteinSequence = new ProteinSequence(sequence);
				sequences.add(proteinSequence);
				final ProteinAccnum accnum = new ProteinAccnum(accessionNumber);
				accnums.add(accnum);
				ProteinDescription desc = descriptions.get(description);
				if (desc == null) {
					desc = new ProteinDescription(description);
					descriptions.put(description, desc);
				}
				final ProteinEntry entry = new ProteinEntry(database, accnum, desc, proteinSequence);
				entries.add(entry);
				if (0 == numSequencesRead % REPORT_FREQUENCY) {
					parsingRange.reportProgress(stream.percentRead());
				}
			}

			initialLoad.reportProgress(0);
			addSequences(sequences, "protein_sequence");
			initialLoad.reportProgress(0.5f);
			addProteinDescriptions(descriptions.values());
			initialLoad.reportProgress(0.75f);
			addProteinAccnums(accnums);
			initialLoad.reportProgress(1.0f);

			int entryNum = 0;
			final int totalEntries = entries.size();
			final SQLQuery sqlQuery = getSession().createSQLQuery("INSERT INTO protein_entry SET curation_id=:curation, protein_accnum_id=:accnum, protein_description_id=:description, protein_sequence_id=:sequence");
			sqlQuery.setParameter("curation", database.getId());

			for (final ProteinEntry entry : entries) {
				entryNum++;

				sqlQuery.setParameter("accnum", entry.getAccessionNumber().getId());
				sqlQuery.setParameter("description", entry.getDescription().getId());
				sqlQuery.setParameter("sequence", entry.getSequence().getId());
				sqlQuery.executeUpdate();

				if (0 == entryNum % REPORT_FREQUENCY) {
					entryLoad.reportProgress((float) entryNum / (float) totalEntries);
				}
			}

			LOGGER.info(MessageFormat.format("Loaded [{0}] to database: {1,number} sequences added.", fasta.getAbsolutePath(), numSequencesRead));
		} catch (final Exception e) {
			throw new MprcException("Could not add FASTA file to database " + database.getTitle(), e);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	@Override
	public void addProteinSequences(final Collection<ProteinSequence> proteinSequences) {
		addSequences(proteinSequences, "protein_sequence");
	}

	private void addSequences(final Collection<? extends Sequence> sequences, final String table) {
		final SequenceBulkLoader loader = new SequenceBulkLoader(this, this, table);
		loader.addObjects(sequences);
	}

	private void addProteinDescriptions(final Collection<ProteinDescription> proteinDescriptions) {
		final ProteinDescriptionBulkLoader loader = new ProteinDescriptionBulkLoader(this, this, "protein_description");
		loader.addObjects(proteinDescriptions);
	}

	private void addProteinAccnums(final Collection<ProteinAccnum> proteinAccnums) {
		final ProteinAccnumBulkLoader loader = new ProteinAccnumBulkLoader(this, this, "protein_accnum");
		loader.addObjects(proteinAccnums);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				HBM_HOME + "ProteinSequence.hbm.xml",
				HBM_HOME + "ProteinEntry.hbm.xml",
				HBM_HOME + "ProteinAccnum.hbm.xml",
				HBM_HOME + "ProteinDescription.hbm.xml",
				HBM_HOME + "TempSequenceLoading.hbm.xml",
				HBM_HOME + "TempStringLoading.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;

	}

	@Override
	public String check() {
		return null;
	}

	@Override
	public void install(final Map<String, String> params) {
		curationDao.install(params);

		if (params.containsKey("test")) {
			final Curation shortTest = curationDao.getCurationByShortName("ShortTest");

			addFastaDatabase(shortTest, null);
		}
	}

	public CurationDao getCurationDao() {
		return curationDao;
	}

	@Resource(name = "curationDao")
	public void setCurationDao(final CurationDao curationDao) {
		this.curationDao = curationDao;
	}
}
