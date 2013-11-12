package edu.mayo.mprc.dbcurator.model.curationsteps;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationExecutor;
import edu.mayo.mprc.dbcurator.model.CurationStatus;
import edu.mayo.mprc.dbcurator.model.StepValidation;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

@Test(sequential = true)
public final class NewDatabaseInclusionTest extends CurationDaoTestBase {
	@DataProvider(name = "urlProvider")
	public Object[][] urlProvider() {
		return new Object[][]{
				{"testDataSource", "classpath:/edu/mayo/mprc/dbcurator/ShortTest.fasta.gz"},
				{"abrfContaminants", "classpath:/edu/mayo/mprc/dbcurator/abrf_contam_2007.fasta.gz"}
		};

	}

	// TODO: re-enable this test
	@Test(dataProvider = "urlProvider", timeOut = 10000L)
	public void performStep_testIPI(final String name, final String testURL) throws InterruptedException {

		final File curationFolder = FileUtilities.createTempFolder();
		final File localTempFolder = FileUtilities.createTempFolder();
		final File curatorArchiveFolder = FileUtilities.createTempFolder();

		if (testURL == null) {
			return;
		}

		final NewDatabaseInclusion step = new NewDatabaseInclusion();
		step.setUrl(testURL);

		final Curation curation = new Curation().addStep(step, -1);
		final CurationExecutor executor = new CurationExecutor(curation, false, curationDao, curationFolder, localTempFolder, curatorArchiveFolder);
		final CurationStatus status = executor.execute();

		while (status.isInProgress()) {
			Thread.sleep(500);
		}

		for (final String msg : status.getMessages()) {
			LOGGER.debug(msg);
		}

		if (status.getFailedStepValidations() != null && !status.getFailedStepValidations().isEmpty()) {
			Assert.fail("The IPI test failed:\n" +
					CurationExecutor.failedValidationsToString(status.getFailedStepValidations()));
		}

		final StepValidation validation = status.getCompletedStepValidations().get(0);

		Assert.assertNotNull(validation, "The validation for the step was null for some reason");
		Assert.assertEquals(validation.getWrappedExceptions().size(), 0, "There was an exception while executing the step");

		FileUtilities.cleanupTempFile(curationFolder);
		FileUtilities.cleanupTempFile(localTempFolder);
		FileUtilities.cleanupTempFile(curatorArchiveFolder);
	}

	private static final Logger LOGGER = Logger.getLogger(NewDatabaseInclusionTest.class);

}
