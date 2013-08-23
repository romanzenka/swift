package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.dbcurator.model.*;
import edu.mayo.mprc.dbcurator.model.curationsteps.MakeDecoyStep;
import edu.mayo.mprc.dbcurator.model.curationsteps.NewDatabaseInclusion;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Initializes the database curator table - creates an initial curation. This is just so Swift comes with at least
 * something out of the box.
 */
public final class CurationInitializer implements RuntimeInitializer {
	private static final Logger LOGGER = Logger.getLogger(CurationInitializer.class);
	public static final String FASTA_FOLDER = "fastaFolder";
	public static final String FASTA_ARCHIVE_FOLDER = "fastaArchiveFolder";

	// Needed for initialization of the database
	private String testURL = "classpath:/edu/mayo/mprc/dbcurator/ShortTest.fasta.gz";
	private String defaultURL = "ftp://ftp.expasy.org/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz";
	private static final int CURATION_UPDATE_SPEED = 5000;
	private CurationDao curationDao;
	private boolean addSprotRev = false;

	public CurationDao getCurationDao() {
		return curationDao;
	}

	public void setCurationDao(final CurationDao curationDao) {
		this.curationDao = curationDao;
	}

	@Override
	public String check(final Map<String, String> params) {
		if (curationDao.countAll(Curation.class) == 0) {
			return "There needs to be at least one FASTA database defined";
		}
		if (curationDao.rowCount(HeaderTransform.class) == 0) {
			return "There needs to be at least one FASTA header transformation preset available";
		}
		return null;
	}

	@Override
	public void initialize(final Map<String, String> params) {
		final File fastaFolder = new File(params.get(FASTA_FOLDER));
		final File fastaArchiveFolder = new File(params.get(FASTA_ARCHIVE_FOLDER));

		HeaderTransform sprotTrans = null;
		HeaderTransform ipiTrans = null;
		HeaderTransform ncbiTrans = null;
		if (curationDao.rowCount(HeaderTransform.class) == 0) {
			LOGGER.info("Filling FASTA header transformation steps table");
			sprotTrans = new HeaderTransform().setName("SwissProt General").setGroupString("^>([^|]*)\\|([^|]*)\\|(.*)$").setSubstitutionPattern(">$3 ($1) ($2)").setCommon(true);
			curationDao.addHeaderTransform(sprotTrans);
			ipiTrans = new HeaderTransform().setName("IPI General").setGroupString("^>IPI:([^.^|^\\s]+)\\S* (Tax_Id=\\S+)?(?:Gene_Symbol=\\S+)?(.*)").setSubstitutionPattern(">$1 $3 $2").setCommon(true);
			curationDao.addHeaderTransform(ipiTrans);
			ncbiTrans = new HeaderTransform().setName("NCBI General").setGroupString("^>(gi\\|([^| ]+)[^\\s]*)\\s([^\\x01\\r\\n]+)(.*)$").setSubstitutionPattern(">gi$2 $3 ;$1 $4").setCommon(true);
			curationDao.addHeaderTransform(ncbiTrans);
		} else {
			sprotTrans = curationDao.getHeaderTransformByName("SwissProt General");
			ipiTrans = curationDao.getHeaderTransformByName("IPI General");
			ncbiTrans = curationDao.getHeaderTransformByName("NCBI General");
		}

		if (curationDao.rowCount(FastaSource.class) == 0) {
			LOGGER.info("Filling FASTA sources");
			if (sprotTrans != null) {
				curationDao.addFastaSource(new FastaSource()
						.setName("Sprot_complete")
						.setUrl("ftp://ftp.expasy.org/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz")
						.setCommon(true)
						.setTransform(sprotTrans));
			}

			if (ipiTrans != null) {
				curationDao.addFastaSource(new FastaSource()
						.setName("IPI_Human")
						.setUrl("ftp://ftp.ebi.ac.uk/pub/databases/IPI/current/ipi.HUMAN.fasta.gz")
						.setCommon(true)
						.setTransform(ipiTrans));

				curationDao.addFastaSource(new FastaSource()
						.setName("IPI_Mouse")
						.setUrl("ftp://ftp.ebi.ac.uk/pub/databases/IPI/current/ipi.MOUSE.fasta.gz")
						.setCommon(true)
						.setTransform(ipiTrans));
			}

			if (ncbiTrans != null) {
				curationDao.addFastaSource(new FastaSource()
						.setName("NCBInr")
						.setUrl("ftp://ftp.ncbi.nih.gov/blast/db/FASTA/nr.gz")
						.setCommon(true)
						.setTransform(ncbiTrans));
			}

			curationDao.addFastaSource(new FastaSource()
					.setName("ShortTest")
					.setUrl("classpath:/edu/mayo/mprc/dbcurator/ShortTest.fasta.gz")
					.setCommon(true)
					.setTransform(null));
		}
		curationDao.flush();

		if (curationDao.countAll(Curation.class) == 0) {
			final Set<Curation> toExecute = new HashSet<Curation>();

			if (testURL != null) {
				//if the database doesn't have a Sprot database then lets create one.
				if (curationDao.getCurationsByShortname("ShortTest").isEmpty()) {
					LOGGER.debug("Creating Curation 'ShortTest' from " + testURL);
					final Curation shortTest = new Curation();
					shortTest.setShortName("ShortTest");

					shortTest.setTitle("Built-in");

					final NewDatabaseInclusion step1 = new NewDatabaseInclusion();
					step1.setUrl(testURL);

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

			if (addSprotRev) {
				LOGGER.debug("Creating curation 'SprotRev' from " + testURL);

				//if the database doesn't have a Sprot database then lets create one.
				if (curationDao.getCurationsByShortname("SprotRev").isEmpty()) {

					final Curation sprotRev = new Curation();
					sprotRev.setShortName("SprotRev");
					sprotRev.setTitle("Built-in");

					if (params.containsKey("forTest")) {
						LOGGER.debug("Creating Curation 'SprotRev' from " + testURL);

						toExecute.add(sprotRev);
						final NewDatabaseInclusion sprotDownload = new NewDatabaseInclusion();
						sprotDownload.setUrl(testURL);
						//the following step is a hack allowing use to specify a header transform before those entries are in the database.
						sprotDownload.setHeaderTransform(new HeaderTransform().setName("SwissProt General").setGroupString("^>([^|]*)\\|(.*)$").setSubstitutionPattern(">$2 ($1)").setCommon(true));
						sprotRev.addStep(sprotDownload, /*position*/-1);

						final MakeDecoyStep decoyStep = new MakeDecoyStep();
						decoyStep.setManipulatorType(MakeDecoyStep.REVERSAL_MANIPULATOR);
						decoyStep.setOverwriteMode(/*overwrite?*/false);
						sprotRev.addStep(decoyStep, /*position*/-1);

					} else {
						LOGGER.debug("Creating Curation 'SprotRev' from " + defaultURL);

						toExecute.add(sprotRev);
						final NewDatabaseInclusion step1 = new NewDatabaseInclusion();
						step1.setUrl(defaultURL);
						sprotRev.addStep(step1, /*position*/-1);

						final MakeDecoyStep step3 = new MakeDecoyStep();
						step3.setManipulatorType(MakeDecoyStep.REVERSAL_MANIPULATOR);
						step3.setOverwriteMode(/*overwrite?*/false);
						sprotRev.addStep(step3, /*position*/-1);
					}
				}
			}

			//execute the ones we decided to execute
			final File localTempFolder = FileUtilities.createTempFolder();
			for (final Curation curation : toExecute) {
				LOGGER.info("Executing curation: " + curation.getShortName());
				final CurationExecutor executor = new CurationExecutor(curation, true, curationDao, fastaFolder, localTempFolder, fastaArchiveFolder);
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

			FileUtilities.cleanupTempFile(localTempFolder);

			LOGGER.info("Done seeding Curation database tables.");
		}
	}
}
