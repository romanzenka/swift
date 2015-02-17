package edu.mayo.mprc.swift.webservice;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.quameterdb.QuameterUi;
import edu.mayo.mprc.quameterdb.dao.QuameterAnnotation;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.utilities.CsvWriter;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Controller
public final class QuameterServices {
	@Resource(name = "quameterDao")
	private QuameterDao quameterDao;

	@Resource(name = "webUiHolder")
	private WebUiHolder webUiHolder;

	@RequestMapping(value = "/service/quameter-hide/{quameterResultId}", method = RequestMethod.POST)
	@ResponseBody
	public void hideQuameterResult(@PathVariable final int quameterResultId, @RequestParam final String reason) {
		quameterDao.begin();
		try {
			quameterDao.hideQuameterResult(quameterResultId, reason);
			quameterDao.commit();
		} catch (final Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not hide quameter result " + quameterResultId, e);
		}
	}

	@RequestMapping(value = "/service/quameter-unhide/{quameterResultId}", method = RequestMethod.POST)
	public String unhideQuameterResult(@PathVariable final int quameterResultId, @RequestParam final String reason) {
		quameterDao.begin();
		try {
			quameterDao.unhideQuameterResult(quameterResultId, reason);
			quameterDao.commit();
		} catch (final Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not un-hide quameter result " + quameterResultId, e);
		}
		return "redirect:/quameter/unhide";
	}

	@RequestMapping(value = "/service/new-annotation", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void createQuameterAnnotation(@RequestBody final MultiValueMap<String, String> inputMap) {
		System.out.println(inputMap.toString());
		quameterDao.begin();
		try {
			quameterDao.addAnnotation(new QuameterAnnotation(inputMap.getFirst("metricCode"), Integer.parseInt(inputMap.getFirst("dbId")), inputMap.getFirst("text")));
			quameterDao.commit();
		} catch (final Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not add quameter annotation.");
		}
	}


	@RequestMapping(value = "/service/list-annotation", method = RequestMethod.GET)
	public ModelAndView getAnnotationList() {
		quameterDao.begin();
		try {
			final List<QuameterAnnotation> myList = quameterDao.listAnnotations();
			quameterDao.commit();
			final ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("quameterannotation", myList);
			return modelAndView;
		} catch (final Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could Not list Quameter Annotations for you.", e);
		}
	}


	//  http://localhost:8080/service/getQuameterDataTable
	@RequestMapping(value = "/service/getQuameterDataTable", method = RequestMethod.GET)
	public void getDataFile(final HttpServletResponse response) {
		quameterDao.begin();
		try {
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=\"quameter.csv\"");
			response.setContentType("application/vnd.ms-excel");

			final List<QuameterProteinGroup> proteinGroups = quameterDao.listProteinGroups();
			final List<QuameterResult> quameterResults = quameterDao.listVisibleResultsAllTime();
			final Writer bw = response.getWriter();
			final CsvWriter writer = new CsvWriter(bw);
			writeHeader(writer, proteinGroups);
			writeRows(writer, quameterResults, proteinGroups);
			FileUtilities.closeQuietly(writer);

			quameterDao.commit();
		} catch (final Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not generate Quameter Data Table for file.", e);
		}
	}

	private void writeHeader(final CsvWriter writer, final List<QuameterProteinGroup> protGrps) throws IOException {
		// Predefined column names
		final List<String> myHeader = Lists.newArrayList("ID", "Start Time", "Path", "Duration (min)", "Category", "Instrument", "Search parameters ID", "SearchRun ID");
		// Quameter Result Column names
		for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
			myHeader.add(QuameterResult.getColumnName(column));
		}
		// Quameter's Protein Group Column Names
		for (final QuameterProteinGroup proteinGroup : protGrps) {
			myHeader.add(proteinGroup.getName());
		}
		final String[] line = new String[myHeader.size()];
		writer.writeNext(myHeader.toArray(line));

		// Make second row with descriptions
		final List<String> myDescriptions = Lists.newArrayList("QuaMeter Entry ID",
				"Acquisition start time for sample",
				"Sample file",
				"Acquisition duration in minutes",
				"User-specified sample category",
				"Instrument serial number",
				"Search Parameters ID",
				"Search Run ID");
		for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
			myDescriptions.add(QuameterUi.getMetricName(column.name()));
		}
		// Quameter's Protein Group Column Names
		for (final QuameterProteinGroup proteinGroup : protGrps) {
			myDescriptions.add(proteinGroup.getName() + " total spectra");
		}
		writer.writeNext(myDescriptions.toArray(line));
	}

	private void writeRows(final CsvWriter writer, final List<QuameterResult> results, final List<QuameterProteinGroup> proteinGroups) throws IOException {

		for (final QuameterResult result : results) {
			final TandemMassSpectrometrySample massSpecSample = result.getSearchResult().getMassSpecSample();
			final SearchEngineParameters parameters = result.getFileSearch().getSearchParameters();
			final Map<QuameterProteinGroup, Integer> identifiedSpectra = result.getIdentifiedSpectra();

			final List<String> myRow = Lists.newArrayList(
					result.getId().toString(), // Id of the entry (for hiding)
					massSpecSample.getStartTime().toString(), // startTime
					massSpecSample.getFile().getAbsolutePath().toString(), // path
					Double.toString(massSpecSample.getRunTimeInSeconds() / 60.0), // duration
					result.getCategory().toString(),
					webUiHolder.getWebUi().mapInstrumentSerialNumbers(massSpecSample.getInstrumentSerialNumber()),
					Integer.toString(parameters != null ? parameters.getId() : 0), // search parameters id
					Integer.toString(result.getTransaction())
			);

			for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
				myRow.add(Double.toString(result.getValue(column)));
			}

			for (final QuameterProteinGroup proteinGroup : proteinGroups) {
				final Integer numSpectra = identifiedSpectra.get(proteinGroup);
				myRow.add(Integer.toString(numSpectra != null ? numSpectra : 0));
			}

			writer.writeNext((String[]) myRow.toArray());
		}
	}


}
