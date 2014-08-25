package edu.mayo.mprc.quameterdb;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Zenka
 */
public final class QuameterUiTest {
	QuameterDao quameterDao;
	QuameterUi quameterUi;

	@BeforeMethod
	public void setup() {
		quameterDao = mock(QuameterDao.class);
		when(quameterDao.listProteinGroups()).thenReturn(quameterProteinGroups());
		when(quameterDao.listAllResults()).thenReturn(quameterResults());

		final QuameterDbWorker.Config quameterDbConfig = new QuameterDbWorker.Config(null, "animal,-cat*,-dog", "", "");

		final QuameterUi.Config config = new QuameterUi.Config();
		config.setQuameterConfig(quameterDbConfig);

		final QuameterUi.Factory factory = new QuameterUi.Factory();
		factory.setQuameterDao(quameterDao);
		quameterUi = factory.create(config, new DependencyResolver(null));
	}

	/**
	 * @return a list of fake  quameter results.
	 */
	private List<QuameterResult> quameterResults() {
		final TandemMassSpectrometrySample sample1 = new TandemMassSpectrometrySample(
				new File("/file/test1.RAW"), new DateTime(2011, 1, 2, 3, 4, 5, 0), 10, 20, 30, "instrument", "serial",
				new DateTime(2011, 2, 3, 10, 20, 30, 0), 20 * 60, "Test File 1", "sample info\nlong\nstring");
		final TandemMassSpectrometrySample sample2 = new TandemMassSpectrometrySample(
				new File("/file/test2.RAW"), new DateTime(2012, 1, 2, 3, 4, 5, 0), 11, 21, 31, "instrument 2", "serial 2",
				new DateTime(2012, 2, 3, 10, 20, 30, 0), 1234, "Test File 2", "sample 2 info\nlong\nstring");

		final SearchEngineParameters p1 = new SearchEngineParameters();
		p1.setId(100);
		final SearchEngineParameters p2 = new SearchEngineParameters();
		p2.setId(101);

		final FileSearch fileSearch1 = new FileSearch(new File("/file/test1.RAW"), "bio sample", null, "experiment", p1);

		final FileSearch fileSearch2 = new FileSearch(new File("/file/test2.RAW"), "bio sample 2", null, "experiment", p2);

		final Map<String, Double> values = QuameterDbWorker.loadQuameterResultFile(
				ResourceUtilities.getReader("classpath:edu/mayo/mprc/quameter/quameter.qual.txt", QuameterLoadTest.class)
		);

		final QuameterResult r1 = new QuameterResult(sample1, fileSearch1, values, getIdentifiedSpectra(1));
		r1.setId(1);
		final QuameterResult r2 = new QuameterResult(sample2, fileSearch2, values, getIdentifiedSpectra(20));
		r2.setId(2);
		return Lists.newArrayList(
				r1,
				r2
		);
	}

	private Map<QuameterProteinGroup, Integer> getIdentifiedSpectra(final int initial) {
		final Map<QuameterProteinGroup, Integer> identifiedSpectra = Maps.newHashMap();

		int i = initial;
		for (final QuameterProteinGroup proteinGroup : quameterProteinGroups()) {
			identifiedSpectra.put(proteinGroup, i);
			i++;
		}
		return identifiedSpectra;
	}

	List<QuameterProteinGroup> quameterProteinGroups() {
		return Lists.newArrayList(
				new QuameterProteinGroup("albumin", "ALBU_HUMAN"),
				new QuameterProteinGroup("keratin", "K1C1_HUMAN"));
	}


	@AfterMethod
	public void teardown() {
	}

	@Test
	public void shouldProduceDataTable() {
		quameterUi.begin();

		final StringWriter writer = new StringWriter(1000);
		try {
			quameterUi.dataTableJson(writer);
			quameterUi.commit();
		} finally {
			FileUtilities.closeQuietly(writer);
		}

		final JsonElement expected = jsonFromString(TestingUtilities.resourceToString("edu/mayo/mprc/quameter/dataTable.json"));
		final JsonElement actual = jsonFromString(writer.toString());

		Assert.assertEquals(actual, expected, "Json objects must match");
	}

	private JsonElement jsonFromString(final String json) {
		final Gson gson = new Gson();
		return gson.fromJson(json, JsonElement.class);
	}


}
