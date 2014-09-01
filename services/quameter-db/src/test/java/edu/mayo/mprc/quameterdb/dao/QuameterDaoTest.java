package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.dbcurator.model.CurationContext;
import edu.mayo.mprc.dbcurator.model.impl.CurationDaoHibernate;
import edu.mayo.mprc.fastadb.FastaDbDaoHibernate;
import edu.mayo.mprc.searchdb.dao.SearchDbDaoHibernate;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.db.SwiftDaoHibernate;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.params2.ParamsDaoHibernate;
import edu.mayo.mprc.unimod.UnimodDaoHibernate;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workspace.WorkspaceDaoHibernate;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.v6.Maps;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class QuameterDaoTest extends DaoTest {
	public static final QuameterProteinGroup ALBUMIN_GROUP = new QuameterProteinGroup("albumin", "ALBU_HUMAN");
	public static final QuameterProteinGroup KERATIN_GROUP = new QuameterProteinGroup("keratin", "K1C10_BOVIN");
	private File tempFolder;

	private WorkspaceDaoHibernate workspaceDao;
	private CurationDaoHibernate curationDao;
	private ParamsDaoHibernate paramsDao;
	private UnimodDaoHibernate unimodDao;
	private SwiftDaoHibernate swiftDao;
	private FastaDbDaoHibernate fastaDbDao;
	private SearchDbDaoHibernate searchDbDao;
	private QuameterDaoHibernate quameterDao;

	private TandemMassSpectrometrySample sample1;
	private TandemMassSpectrometrySample sample2;
	private FileSearch fileSearch1;
	private FileSearch fileSearch2;

	@BeforeMethod
	public void init() {
		tempFolder = FileUtilities.createTempFolder();

		workspaceDao = new WorkspaceDaoHibernate();
		final CurationContext curationContext = new CurationContext();
		curationContext.initialize(
				new File(tempFolder, "fasta"),
				new File(tempFolder, "fastaUpload"),
				new File(tempFolder, "fastaArchive"),
				new File(tempFolder, "localTemp"));

		curationDao = new CurationDaoHibernate(curationContext);
		paramsDao = new ParamsDaoHibernate(workspaceDao, curationDao);
		unimodDao = new UnimodDaoHibernate();
		swiftDao = new SwiftDaoHibernate(workspaceDao, curationDao, paramsDao, unimodDao);
		fastaDbDao = new FastaDbDaoHibernate(curationDao);
		searchDbDao = new SearchDbDaoHibernate(swiftDao, fastaDbDao);
		quameterDao = new QuameterDaoHibernate(swiftDao, searchDbDao);

		initializeDatabase(Arrays.asList(workspaceDao, curationDao, paramsDao, unimodDao, swiftDao, fastaDbDao, searchDbDao, quameterDao));

		quameterDao.begin();
		try {
			final Map<String, String> testMap = Maps.newHashMap();
			testMap.put("test", "true");
			quameterDao.install(testMap);

			/* Load existing samples */
			sample1 = searchDbDao.getTandemMassSpectrometrySampleForId(1);
			sample2 = searchDbDao.getTandemMassSpectrometrySampleForId(2);

			fileSearch1 = swiftDao.getFileSearchForId(1);
			fileSearch2 = swiftDao.getFileSearchForId(2);
			quameterDao.commit();
		} catch (final Exception e) {
			quameterDao.rollback();
			throw new MprcException(e);
		}
	}

	@AfterMethod
	public void teardown() {
		FileUtilities.cleanupTempFile(tempFolder);
	}

	List<QuameterProteinGroup> quameterProteinGroups() {
		return Lists.newArrayList(
				ALBUMIN_GROUP,
				KERATIN_GROUP);
	}

	@Test
	public void shouldAddResult() {
		quameterDao.begin();

		final QuameterResult quameterResult = addResult1();

		nextTransaction();

		Assert.assertEquals(quameterResult.getMs2_4a(), 1.22);

		final List<QuameterResult> quameterResults = quameterDao.listAllResults();
		Assert.assertEquals(quameterResults.size(), 1);

		quameterDao.commit();
	}

	@Test
	public void shouldHideResult() {
		quameterDao.begin();

		final QuameterResult quameterResult = addResult1();

		nextTransaction();

		quameterDao.hideQuameterResult(quameterResult.getId());

		nextTransaction();

		final List<QuameterResult> quameterResults = quameterDao.listAllResults();
		Assert.assertEquals(quameterResults.size(), 0);

		quameterDao.commit();
	}

	@Test
	public void shouldFilterOutputs() {
		quameterDao.begin();

		addResult1();

		nextTransaction();

		addResult2();

		nextTransaction();

		final List<QuameterResult> quameterResults = quameterDao.listAllResults();
		Assert.assertEquals(quameterResults.size(), 2);

		quameterDao.commit();
	}

	@Test
	public void shouldAddAnnotation() {
		quameterDao.begin();

		final QuameterResult quameterResult = addResult1();

		nextTransaction();

		final QuameterAnnotation annotation = new QuameterAnnotation(QuameterResult.QuameterColumn.c_3a, quameterResult.getId(), "annotation text");
		quameterDao.addAnnotation(annotation);

		nextTransaction();

		final List<QuameterAnnotation> quameterAnnotations = quameterDao.listAnnotations();
		Assert.assertEquals(quameterAnnotations.size(), 1, "Must get 1 annotation that was just added");
		Assert.assertEquals(quameterAnnotations.get(0).getText(), "annotation text", "Annotation text must be stored properly");

		quameterDao.commit();
	}

	@Test
	public void shouldReplaceAnnotation() {
		quameterDao.begin();

		final QuameterResult quameterResult = addResult1();

		nextTransaction();

		final QuameterAnnotation annotation = new QuameterAnnotation(QuameterResult.QuameterColumn.c_3a, quameterResult.getId(), "annotation text");
		quameterDao.addAnnotation(annotation);

		nextTransaction();

		// Replace annotation
		final QuameterAnnotation annotation2 = new QuameterAnnotation(QuameterResult.QuameterColumn.c_3a, quameterResult.getId(), "annotation text replacement");
		quameterDao.addAnnotation(annotation2);

		nextTransaction();

		final List<QuameterAnnotation> quameterAnnotations = quameterDao.listAnnotations();
		Assert.assertEquals(quameterAnnotations.size(), 1, "Must get 1 annotation that was just added");
		Assert.assertEquals(quameterAnnotations.get(0).getText(), "annotation text replacement", "The existing annotation must be replaced");

		quameterDao.commit();
	}

	@Test
	public void shouldNotListAnnotationOnHiddenResults() {
		quameterDao.begin();

		final QuameterResult quameterResult1 = addResult1();
		final QuameterResult quameterResult2 = addResult2();

		nextTransaction();

		final QuameterAnnotation annotation = new QuameterAnnotation(QuameterResult.QuameterColumn.c_3a, quameterResult1.getId(), "annotation text shown");
		quameterDao.addAnnotation(annotation);

		final QuameterAnnotation annotation2 = new QuameterAnnotation(QuameterResult.QuameterColumn.c_3a, quameterResult2.getId(), "annotation text hidden");
		quameterDao.addAnnotation(annotation2);

		nextTransaction();

		// Hiden result 2
		quameterDao.hideQuameterResult(quameterResult2.getId());

		nextTransaction();

		final List<QuameterAnnotation> quameterAnnotations = quameterDao.listAnnotations();
		Assert.assertEquals(quameterAnnotations.size(), 1, "Must get 1 annotation only (1 is hidden)");
		Assert.assertEquals(quameterAnnotations.get(0).getText(), "annotation text shown", "Only the shown result must be listed");

		quameterDao.commit();
	}

	@Test
	public void shouldManageQuameterProteinGroups() {
		quameterDao.begin(); // Initial load

		final List<QuameterProteinGroup> groups = Arrays.asList(
				new QuameterProteinGroup("hello", "world"),
				new QuameterProteinGroup("group2", "regex2")
		);
		quameterDao.updateProteinGroups(groups);

		nextTransaction(); // List what is in database

		final List<QuameterProteinGroup> proteinGroups = quameterDao.listProteinGroups();
		Assert.assertEquals(proteinGroups.size(), 2);

		nextTransaction(); // Update list

		final List<QuameterProteinGroup> groups2 = Arrays.asList(
				new QuameterProteinGroup("hello", "world"),
				new QuameterProteinGroup("group3", "regex3")
		);
		quameterDao.updateProteinGroups(groups2);

		nextTransaction(); // List what is in database

		final List<QuameterProteinGroup> proteinGroups2 = quameterDao.listProteinGroups();
		Assert.assertEquals(proteinGroups.size(), 2);

		quameterDao.commit();
	}

	private QuameterResult addResult1() {
		return quameterDao.addQuameterScores(sample1.getId(), fileSearch1.getId(), new ImmutableMap.Builder<String, Double>()
				.put("MS2-4A", 1.22)
				.put("C-1A", 0.0)
				.build(), null);
	}

	private QuameterResult addResult2() {
		return quameterDao.addQuameterScores(sample2.getId(), fileSearch2.getId(), new ImmutableMap.Builder<String, Double>()
				.put("MS2-4A", 2.33)
				.put("C-1A", 1.2)
				.build(), null);
	}

	@Test
	public void shouldUpdateQuameterProteinGroups() {
		quameterDao.begin();
		try {

			List<QuameterProteinGroup> proteinGroups = quameterDao.updateProteinGroups(quameterProteinGroups());
			Assert.assertEquals(proteinGroups.size(), 2);
			for (QuameterProteinGroup proteinGroup : proteinGroups) {
				Assert.assertNotNull(proteinGroup.getId());
			}
			quameterDao.commit();
		} catch (Exception e) {
			quameterDao.rollback();
		}
	}

	@Test
	public void shouldCalculateProteinCounts() {
		quameterDao.begin();

		quameterDao.updateProteinGroups(quameterProteinGroups());

		nextTransaction();

		List<QuameterProteinGroup> proteinGroups = quameterDao.listProteinGroups();
		final Map<QuameterProteinGroup, Integer> identifiedSpectra = quameterDao.getIdentifiedSpectra(1, 1, 1, proteinGroups);

		quameterDao.commit();

		Assert.assertEquals(identifiedSpectra.size(), 2, "We need count for each protein group");
		Assert.assertEquals(identifiedSpectra.get(ALBUMIN_GROUP), Integer.valueOf(0), "Number of albumin spectra must match");
		Assert.assertEquals(identifiedSpectra.get(KERATIN_GROUP), Integer.valueOf(2), "Number of keratin spectra must match");
	}
}
