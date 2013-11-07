package edu.mayo.mprc.fastadb;

import com.google.common.base.Preconditions;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.fasta.FASTAInputStream;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.PercentDoneReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Hibernate implementation of {@link FastaDbDao}.
 *
 * @author Roman Zenka
 */
@Repository("fastaDbDao")
public final class FastaDbDaoHibernate extends DaoBase implements FastaDbDao {
	private static final Logger LOGGER = Logger.getLogger(FastaDbDaoHibernate.class);
	// Progress will be checked each X spectra
	public static final long REPORT_FREQUENCY = 100L;
	private static final String HBM_HOME = "edu/mayo/mprc/fastadb/";

	public FastaDbDaoHibernate() {
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
	public String getProteinDescription(final Curation database, final String accessionNumber) {
		try {
			return (String) getSession()
					.createQuery("select e.description.description from ProteinEntry e where e.accessionNumber.accnum = :accessionNumber and e.database=:database")
					.setParameter("accessionNumber", accessionNumber)
					.setParameter("database", database)
					.uniqueResult();
		} catch (Exception e) {
			throw new MprcException(MessageFormat.format("Could not find description for protein [{0}] in database [{1}]", accessionNumber, database.getShortName()), e);
		}
	}

	@Override
	public ProteinSequence addProteinSequence(final ProteinSequence proteinSequence) {
		if (null == proteinSequence.getId()) {
			return save(proteinSequence, nullSafeEq("sequence", proteinSequence.getSequence()), false);
		}
		return proteinSequence;
	}

	private ProteinSequence addProteinSequence(final StatelessSession session, final ProteinSequence proteinSequence) {
		if (null == proteinSequence.getId()) {
			return saveStateless(session, proteinSequence, nullSafeEq("sequence", proteinSequence.getSequence()), false);
		}
		return proteinSequence;
	}

	private ProteinAccnum addAccessionNumber(final StatelessSession session, final ProteinAccnum accessionNumber) {
		if (null == accessionNumber.getId()) {
			return saveStateless(session, accessionNumber, nullSafeEq("accnum", accessionNumber.getAccnum()), false);
		}
		return accessionNumber;
	}

	private ProteinDescription addDescription(final StatelessSession session, final ProteinDescription description) {
		if (null == description.getId()) {
			return saveStateless(session, description, nullSafeEq("description", description.getDescription()), false);
		}
		return description;
	}

	@Override
	public ProteinSequence getProteinSequence(final int proteinId) {
		return (ProteinSequence) getSession().get(ProteinSequence.class, proteinId);
	}

	@Override
	public PeptideSequence addPeptideSequence(final PeptideSequence peptideSequence) {
		if (null == peptideSequence.getId()) {
			return save(peptideSequence, nullSafeEq("sequence", peptideSequence.getSequence()), false);
		}
		return peptideSequence;
	}

	@Override
	public PeptideSequence getPeptideSequence(final int peptideId) {
		return (PeptideSequence) getSession().get(PeptideSequence.class, peptideId);
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
		final StatelessSession session = getDatabase().getSessionFactory().openStatelessSession();
		final Query entryCount = session.createQuery("select count(*) from ProteinEntry p where p.database=:database").setEntity("database", database);
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
		try {
			session.getTransaction().begin();
			stream.beforeFirst();
			long numSequencesRead = 0L;
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
				final ProteinSequence proteinSequence = addProteinSequence(session, new ProteinSequence(sequence));
				final ProteinAccnum accnum = addAccessionNumber(session, new ProteinAccnum(accessionNumber));
				final ProteinDescription desc = addDescription(session, new ProteinDescription(description));
				final ProteinEntry entry = new ProteinEntry(database, accnum, desc, proteinSequence);
				// We know that we will never save two identical entries (fasta has each entry unique and we have not
				// loaded the database yet. So no need to check)
				saveStateless(session, entry, null, false);
				if (0 == numSequencesRead % REPORT_FREQUENCY) {
					percentReporter.reportProgress(stream.percentRead());
				}
			}
			LOGGER.info(MessageFormat.format("Loaded [{0}] to database: {1,number} sequences added.", fasta.getAbsolutePath(), numSequencesRead));
			session.getTransaction().commit();
		} catch (Exception e) {
			session.getTransaction().rollback();
			throw new MprcException("Could not add FASTA file to database " + database.getTitle(), e);
		} finally {
			FileUtilities.closeQuietly(stream);
			session.close();
		}
	}

	@Override
	public void addProteinSequences(final Collection<ProteinSequence> proteinSequences) {
		addSequences(proteinSequences, "protein_sequence");
	}

	@Override
	public void addPeptideSequences(final Collection<PeptideSequence> peptideSequences) {
		addSequences(peptideSequences, "peptide_sequence");
	}

	private void addSequences(final Collection<? extends Sequence> sequences, final String table) {
		SequenceBulkLoader loader = new SequenceBulkLoader(this, this, table);
		loader.addObjects(sequences);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList(
				HBM_HOME + "PeptideSequence.hbm.xml",
				HBM_HOME + "ProteinSequence.hbm.xml",
				HBM_HOME + "ProteinEntry.hbm.xml",
				HBM_HOME + "ProteinAccnum.hbm.xml",
				HBM_HOME + "ProteinDescription.hbm.xml",
				HBM_HOME + "TempSequenceLoading.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;

	}
}
