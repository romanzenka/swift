package edu.mayo.mprc.dbcurator.model.impl;

import com.google.common.collect.Lists;
import edu.mayo.mprc.database.DummyFileTokenTranslator;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationStep;
import edu.mayo.mprc.dbcurator.model.curationsteps.*;
import edu.mayo.mprc.integration.Installer;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public final class CurationDaoHibernateTest extends CurationDaoTestBase {

	private File fastaFileFolder;
	private File testFasta;
	private File uploadedFasta;

	@BeforeClass
	public void installFiles() {
		fastaFileFolder = Installer.testFastaFiles(null, Installer.Action.INSTALL);
		testFasta = new File(fastaFileFolder, "test_in.fasta");
		uploadedFasta = new File(fastaFileFolder, "uploaded.fasta");
	}

	@AfterClass
	public void cleanupFiles() {
		Installer.testFastaFiles(fastaFileFolder, Installer.Action.UNINSTALL);
	}

	@Test
	public void shouldExtractUniqueName() {
		Assert.assertEquals(CurationDaoHibernate.extractShortName("${DBPath:hello}"), "hello");
		Assert.assertEquals(CurationDaoHibernate.extractShortName("${DB:this is a test 123}"), "this is a test 123");
		Assert.assertEquals(CurationDaoHibernate.extractShortName("{DBPath:aaaa}"), "{DBPath:aaaa}");
		Assert.assertEquals(CurationDaoHibernate.extractShortName("${DBPath:aaaa_LATEST}"), "aaaa_LATEST");
	}

	@Test
	public void shouldExtractShortName() {
		Assert.assertEquals(CurationDaoHibernate.extractShortname("${DBPath:hello}"), "${DBPath:hello}");
		Assert.assertEquals(CurationDaoHibernate.extractShortname("${DB:this is a test 123_LATEST}"), "this is a test 123");
		Assert.assertEquals(CurationDaoHibernate.extractShortname("database20090102A.fasta"), "database");
		Assert.assertEquals(CurationDaoHibernate.extractShortname("Sprot2219920304C.FASTA"), "Sprot22");
	}

	@Test
	public void shouldSaveAndLoadCurations() {
		// We want to make sure we go through translator
		DummyFileTokenTranslator translator = new DummyFileTokenTranslator();
		curationDao.getDatabase().setTranslator(translator);

		curationDao.begin();
		final Curation db = new Curation();
		db.setShortName("hellodb");
		db.setTitle("Hello Database");
		db.setNotes("A test database");
		db.setRunDate(new DateTime());
		db.setCurationFile(testFasta);
		db.setOwnerEmail("zenka.roman@mayo.edu");

		db.clearSteps();
		final List<CurationStep> steps = Lists.newArrayList(
				new ManualInclusionStep("ALBU_HUMAN", "MKWVTFISLLFLFSSAYSRGVFRRDAHKSEVAHRFKDLGEENFKALVLIA", 1),
				new DatabaseUploadStep(uploadedFasta, "C:\\to_upload.fasta", 11),
				new HeaderFilterStep(edu.mayo.mprc.fasta.filter.MatchMode.ANY, TextMode.REG_EX, "pi\\|pe|pipe", 5),
				new HeaderTransformStep("transform", "a\\|b", "$1", 13),
				new MakeDecoyStep(true, MakeDecoyStep.REVERSAL_MANIPULATOR, 20),
				new NewDatabaseInclusion("http://test", 30));

		for (final CurationStep step : steps) {
			db.addStep(step, -1);
		}

		curationDao.addCuration(db);

		// Check translation counts
		Assert.assertEquals(translator.getNumDbToFile(), 0, "No deserialization");
		Assert.assertEquals(translator.getNumFileToDb(), 1, "One translation db->file for DatabaseUploadStep pathToUploadedFile");
		translator = new DummyFileTokenTranslator();
		curationDao.getDatabase().setTranslator(translator);

		nextTransaction();

		final Curation hellodb = curationDao.getCurationByShortName("hellodb");
		Assert.assertEquals(hellodb.getCurationSteps().size(), 6);
		for (int i = 0; i < steps.size(); i++) {
			Assert.assertEquals(hellodb.getCurationSteps().get(i), steps.get(i), "Step #" + i + " must match");
		}


		curationDao.commit();

		// Check translation counts
		Assert.assertEquals(translator.getNumFileToDb(), 0, "No serialization");
		Assert.assertEquals(translator.getNumDbToFile(), 1, "One translation file->db for DatabaseUploadStep pathToUploadedFile");
	}
}
