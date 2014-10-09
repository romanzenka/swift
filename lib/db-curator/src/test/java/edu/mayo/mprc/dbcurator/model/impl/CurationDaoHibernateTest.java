package edu.mayo.mprc.dbcurator.model.impl;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.curationsteps.*;
import edu.mayo.mprc.integration.Installer;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

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
		curationDao.begin();
		final Curation db = new Curation();
		db.setShortName("hellodb");
		db.setTitle("Hello Database");
		db.setNotes("A test database");
		db.setRunDate(new DateTime());
		db.setCurationFile(testFasta);
		db.setOwnerEmail("zenka.roman@mayo.edu");

		db.clearSteps();
		db.addStep(new ManualInclusionStep("ALBU_HUMAN", "MKWVTFISLLFLFSSAYSRGVFRRDAHKSEVAHRFKDLGEENFKALVLIA", 1), -1);
		db.addStep(new DatabaseUploadStep(uploadedFasta, "C:\\to_upload.fasta", new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 11), -1);
		db.addStep(new HeaderFilterStep(edu.mayo.mprc.fasta.filter.MatchMode.ANY, TextMode.REG_EX, "pi\\|pe|pipe", 5), -1);
		curationDao.addCuration(db);

		nextTransaction();

		final Curation hellodb = curationDao.getCurationByShortName("hellodb");
		Assert.assertEquals(hellodb.getCurationSteps().size(), 3);

		curationDao.commit();
	}
}
