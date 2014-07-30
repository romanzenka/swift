package edu.mayo.mprc.dbcurator;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationExecutor;
import edu.mayo.mprc.dbcurator.model.CurationStatus;
import edu.mayo.mprc.dbcurator.model.curationsteps.*;
import edu.mayo.mprc.fasta.filter.MatchMode;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertTrue;


/**
 * A set of test cases for testing a few different routine curations
 */
public final class CurationExecutorTest extends CurationDaoTestBase {

	private static final Logger LOGGER = Logger.getLogger(CurationExecutorTest.class);
	File curationFolder;
	File localTempFolder;
	File curatorArchiveFolder;
	Curation curation;

	@BeforeMethod
	public void beforeMethod() {
		curationFolder = FileUtilities.createTempFolder();
		localTempFolder = FileUtilities.createTempFolder();
		curatorArchiveFolder = FileUtilities.createTempFolder();

		curation = new Curation();
		curation.setShortName("FirstTests");
		curation.setTitle("The first test cases");
		curation.setNotes("This is just a test curation that will be run as a unit test.");
	}

	@AfterMethod
	public void afterMethod() {
		FileUtilities.cleanupTempFile(curationFolder);
		FileUtilities.cleanupTempFile(localTempFolder);
		FileUtilities.cleanupTempFile(curatorArchiveFolder);
	}

	@Test(groups = {"fast", "integration"}, enabled = true)
	public void testSimpleCurationCreationAndExecution() {
		curationDao.begin();
		try {
			final String url = "classpath:/edu/mayo/mprc/dbcurator/ShortTest.fasta.gz";

			final NewDatabaseInclusion inclusionStep = new NewDatabaseInclusion();
			inclusionStep.setUrl(url);
			assertTrue(inclusionStep.preValidate(curationDao).isOK(), "The NewDatabaseInclusion step failed validation.");

			final ManualInclusionStep manualInclusion = new ManualInclusionStep();
			manualInclusion.setHeader(">MyManualInclusion_HUMAN");
			manualInclusion.setSequence("LLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

			//just a basic header filter will be used so only sequences with the string "_human" will be retained
			final HeaderFilterStep filterStep = new HeaderFilterStep();
			filterStep.setTextMode(TextMode.REG_EX);
			filterStep.setCriteriaString("^>[^ ]+_HUMAN");
			filterStep.setMatchMode(MatchMode.ANY);
			assertTrue(filterStep.preValidate(curationDao).isOK(), "The filter failed prevalidation");

			//add a sequence scramble step to this database this will take the file write its contents and then append
			//the same sequences with sequences scrambled up.
			final MakeDecoyStep scrambleStep = new MakeDecoyStep();
			scrambleStep.setManipulatorType(MakeDecoyStep.REVERSAL_MANIPULATOR);
			scrambleStep.setOverwriteMode(false); //append the reversed sequences to the file
			assertTrue(scrambleStep.preValidate(curationDao).isOK(), "The scramble step failed prevalidation.");

			//add the steps to the curation
			curation.addStep(manualInclusion, -1);
			curation.addStep(inclusionStep, -1); //add the step to the end of the curation
			curation.addStep(filterStep, -1);
			curation.addStep(scrambleStep, -1); //add manipulation step to the; //add the step to the end of the curation

			//get the object that we will use to keep track of the executor's progress
			final CurationStatus status = executeCuration();

			//if we had a failure then let's figure out why
			if (status.getFailedStepValidations() != null && !status.getFailedStepValidations().isEmpty()) {
				Assert.fail("There were errors executing the curation.\n" +
						CurationExecutor.failedValidationsToString(status.getFailedStepValidations()));
			}

			//get the final messages from the executor
			LOGGER.info(status.getMessages());

			LOGGER.info(curation.simpleDescription());
			curationDao.commit();
		} catch (Exception e) {
			curationDao.rollback();
			throw new MprcException(e);
		}
	}

	@Test(groups = {"fast", "integration"}, enabled = true)
	public void shouldFailWhenInvalidFastaCreated() {
		curationDao.begin();
		try {
			final ManualInclusionStep manualInclusion = new ManualInclusionStep();
			manualInclusion.setHeader(">MyManualInclusion_HUMAN");
			manualInclusion.setSequence("LLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

			final ManualInclusionStep manualInclusion2 = new ManualInclusionStep();
			manualInclusion2.setHeader(">MyManualInclusion_HUMAN");
			manualInclusion2.setSequence("*#@)!?");

			//add the steps to the curation
			curation.addStep(manualInclusion, -1);
			curation.addStep(manualInclusion2, -1);
			final CurationStatus status = executeCuration();

			Assert.assertEquals(status.getCompletedStepValidations().size(), 1);
			Assert.assertEquals(status.getFailedStepValidations().size(), 1);
			Assert.assertEquals(status.getFailedStepValidations().get(0).getMessages().get(0), "Duplicate accession number: [MyManualInclusion_HUMAN] (line #3)");

			//get the final messages from the executor
			LOGGER.info(status.getMessages());

			LOGGER.info(curation.simpleDescription());
			curationDao.commit();
		} catch (Exception e) {
			curationDao.rollback();
			throw new MprcException(e);
		}
	}

	private CurationStatus executeCuration() {
		final CurationExecutor executor = new CurationExecutor(curation, false, curationDao, curationFolder, localTempFolder, curatorArchiveFolder);
		final CurationStatus status = executor.execute();

		waitForCurationToComplete(status);
		return status;
	}

	/**
	 * every 0.5 seconds output the progress of the curation and any messages that were produced
	 */
	private void waitForCurationToComplete(CurationStatus status) {
		while (status.isInProgress()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.error(e);
			}
			LOGGER.info(status.getMessages());
			LOGGER.info("Step " + status.getCurrentStepNumber() + " is " + status.getCurrentStepProgress() + "% complete");
		}
	}

}
