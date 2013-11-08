package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDaoImpl;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Exercises the fasta-db DAO.
 *
 * @author Roman Zenka
 */
public class TestFastaDbDao extends DaoTest {
	private FastaDbDaoHibernate fastaDbDao;
	private CurationDaoImpl curationDao;

	@BeforeMethod
	public void setup() {
		fastaDbDao = new FastaDbDaoHibernate();
		curationDao = new CurationDaoImpl();

		initializeDatabase(Arrays.asList(fastaDbDao, curationDao));
	}

	@AfterMethod
	public void teardown() {
		teardownDatabase();
	}

	@Test
	public void shouldLoadFasta() throws IOException, SQLException {
		final Curation currentSp = loadFasta("/edu/mayo/mprc/fastadb/test.fasta", "Current_SP");
		// Curation currentSp = loadFasta(new File("/Users/m044910/Documents/Databases/Current_SP.fasta"), "Current_SP");

		fastaDbDao.begin();
		Assert.assertEquals(fastaDbDao.countDatabaseEntries(currentSp), 9);
		fastaDbDao.commit();

		// Add the same thing again. Nothing should happen.
		fastaDbDao.addFastaDatabase(currentSp, null);

		fastaDbDao.begin();
		Assert.assertEquals(fastaDbDao.countDatabaseEntries(currentSp), 9);
		fastaDbDao.commit();
	}

	@Test
	public void shouldResolveDescription() throws IOException, SQLException {
		final Curation currentSp = loadFasta("/edu/mayo/mprc/fastadb/test.fasta", "Current_SP");

		fastaDbDao.begin();
		Assert.assertEquals(fastaDbDao.getProteinDescription(currentSp, "K1C9_HUMAN"), "Keratin, type I cytoskeletal 9 OS=Homo sapiens GN=KRT9 PE=1 SV=3 (sp) (P35527)");
		fastaDbDao.commit();
	}

	@Test
	public void shouldSaveUniqueProtein() {
		fastaDbDao.begin();
		final ProteinSequence seq1 = new ProteinSequence("STNQR");
		final ProteinSequence seq2 = new ProteinSequence(" stnqr* ");
		Assert.assertEquals(seq1, seq2, "Same sequence written in two ways");

		final ProteinSequence seq1saved = fastaDbDao.addProteinSequence(seq1);
		final ProteinSequence seq2saved = fastaDbDao.addProteinSequence(seq2);

		Assert.assertEquals(seq1saved.getId(), seq2saved.getId(), "Two sequences saved as single row in database");

		final Integer seq1savedId = seq1saved.getId();
		fastaDbDao.addProteinSequence(seq1saved);
		Assert.assertEquals(seq1saved.getId(), seq1savedId, "The ID does not change on double save");

		fastaDbDao.commit();

		fastaDbDao.begin();
		final ProteinSequence seq1Loaded = fastaDbDao.getProteinSequence(seq1savedId);
		Assert.assertEquals(seq1Loaded, seq1, "Load has to work");
		fastaDbDao.commit();

		Assert.assertEquals(seq1.hashCode(), seq2.hashCode(), "Hashes of identical sequences must match");
	}

	@Test
	public void shouldSaveUniquePeptide() {
		fastaDbDao.begin();
		final PeptideSequence seq1 = new PeptideSequence("\t  bBBZ  ");
		final PeptideSequence seq2 = new PeptideSequence(" Bbbz* ");
		Assert.assertEquals(seq1, seq2, "Same sequence written in two ways");

		final PeptideSequence seq1saved = fastaDbDao.addPeptideSequence(seq1);
		final PeptideSequence seq2saved = fastaDbDao.addPeptideSequence(seq2);

		Assert.assertEquals(seq1saved.getId(), seq2saved.getId(), "Two sequences saved as single row in database");

		final Integer seq1savedId = seq1saved.getId();
		fastaDbDao.addPeptideSequence(seq1saved);
		Assert.assertEquals(seq1saved.getId(), seq1savedId, "The ID does not change on double save");

		fastaDbDao.commit();

		fastaDbDao.begin();
		final PeptideSequence seq1Loaded = fastaDbDao.getPeptideSequence(seq1savedId);
		Assert.assertEquals(seq1Loaded, seq1, "Load has to work");
		fastaDbDao.commit();
	}

	@Test
	public void shouldTranslateAccessionNumbers() {
		loadFasta("/edu/mayo/mprc/fastadb/test.fasta", "Current_SP");
		fastaDbDao.begin();
		try {
			final ProteinSequenceTranslator translator = new SingleDatabaseTranslator(fastaDbDao, curationDao);
			final ProteinSequence proteinSequence = translator.getProteinSequence("K1C9_HUMAN", "Current_SP.fasta.gz");
			Assert.assertEquals(proteinSequence.getSequence(),
					"MSCRQFSSSYLSRSGGGGGGGLGSGGSIRSSYSRFSSSGGGGGGGRFSSSSGYGGGSSRVCGRGGGGSFGYSYGGGSGGG" +
							"FSASSLGGGFGGGSRGFGGASGGGYSSSGGFGGGFGGGSGGGFGGGYGSGFGGFGGFGGGAGGGDGGILTANEKSTMQEL" +
							"NSRLASYLDKVQALEEANNDLENKIQDWYDKKGPAAIQKNYSPYYNTIDDLKDQIVDLTVGNNKTLLDIDNTRMTLDDFR" +
							"IKFEMEQNLRQGVDADINGLRQVLDNLTMEKSDLEMQYETLQEELMALKKNHKEEMSQLTGQNSGDVNVEINVAPGKDLT" +
							"KTLNDMRQEYEQLIAKNRKDIENQYETQITQIEHEVSSSGQEVQSSAKEVTQLRHGVQELEIELQSQLSKKAALEKSLED" +
							"TKNRYCGQLQMIQEQISNLEAQITDVRQEIECQNQEYSLLLSIKMRLEKEIETYHNLLEGGQEDFESSGAGKIGLGGRGG" +
							"SGGSYGRGSRGGSGGSYGGGGSGGGYGGGSGSRGGSGGSYGGGSGSGGGSGGGYGGGSGGGHSGGSGGGHSGGSGGNYGG" +
							"GSGSGGGSGGGYGGGSGSRGGSGGSHGGGSGFGGESGGSYGGGEEASGSGGGYGGGSGKSSHS");

			final ProteinSequence sequence2 = translator.getProteinSequence("nonexistent", "Current_SP");
			Assert.assertNull(sequence2, "No sequence found means null should be returned");
		} finally {
			fastaDbDao.commit();
		}
	}

	/**
	 * Mascot can report database name with additional timestamp attached to the end.
	 */
	@Test
	public void shouldSupportMascotDatabaseSuffixes() {
		loadFasta("/edu/mayo/mprc/fastadb/test.fasta", "Current_SP");
		fastaDbDao.begin();
		try {
			final ProteinSequenceTranslator translator = new SingleDatabaseTranslator(fastaDbDao, curationDao);
			final ProteinSequence proteinSequence = translator.getProteinSequence("K1C9_HUMAN", "Current_SP_20120716.fasta.gz");
			Assert.assertEquals(proteinSequence.getSequence(),
					"MSCRQFSSSYLSRSGGGGGGGLGSGGSIRSSYSRFSSSGGGGGGGRFSSSSGYGGGSSRVCGRGGGGSFGYSYGGGSGGG" +
							"FSASSLGGGFGGGSRGFGGASGGGYSSSGGFGGGFGGGSGGGFGGGYGSGFGGFGGFGGGAGGGDGGILTANEKSTMQEL" +
							"NSRLASYLDKVQALEEANNDLENKIQDWYDKKGPAAIQKNYSPYYNTIDDLKDQIVDLTVGNNKTLLDIDNTRMTLDDFR" +
							"IKFEMEQNLRQGVDADINGLRQVLDNLTMEKSDLEMQYETLQEELMALKKNHKEEMSQLTGQNSGDVNVEINVAPGKDLT" +
							"KTLNDMRQEYEQLIAKNRKDIENQYETQITQIEHEVSSSGQEVQSSAKEVTQLRHGVQELEIELQSQLSKKAALEKSLED" +
							"TKNRYCGQLQMIQEQISNLEAQITDVRQEIECQNQEYSLLLSIKMRLEKEIETYHNLLEGGQEDFESSGAGKIGLGGRGG" +
							"SGGSYGRGSRGGSGGSYGGGGSGGGYGGGSGSRGGSGGSYGGGSGSGGGSGGGYGGGSGGGHSGGSGGGHSGGSGGNYGG" +
							"GSGSGGGSGGGYGGGSGSRGGSGGSHGGGSGFGGESGGSYGGGEEASGSGGGYGGGSGKSSHS");

			final ProteinSequence sequence2 = translator.getProteinSequence("nonexistent", "Current_SP_20120716");
			Assert.assertNull(sequence2, "No sequence found means null should be returned");
		} finally {
			fastaDbDao.commit();
		}
	}

	private Curation loadFasta(final String resource, final String shortName) {
		File file = null;
		try {
			file = TestingUtilities.getTempFileFromResource(resource, true, null);
			return loadFasta(file, shortName);
		} catch (Exception e) {
			throw new MprcException("Failed to load database [" + shortName + "]", e);
		} finally {
			FileUtilities.cleanupTempFile(file);
		}
	}

	private Curation loadFasta(final File file, final String shortName) {
		try {
			final Curation curation = addCurationToDatabase(shortName, file);
			fastaDbDao.addFastaDatabase(curation, null);
			return curation;
		} catch (Exception e) {
			throw new MprcException("Failed to load database [" + shortName + "]", e);
		}
	}

	private Curation addCurationToDatabase(final String databaseName, final File currentSpFasta) {
		Curation currentSp = null;
		try {
			curationDao.begin();
			currentSp = new Curation();
			currentSp.setShortName(databaseName);
			currentSp.setCurationFile(currentSpFasta);
			curationDao.addCuration(currentSp);
			curationDao.commit();
		} catch (Exception e) {
			org.testng.Assert.fail("Cannot load fasta database", e);
		}
		return currentSp;
	}
}
