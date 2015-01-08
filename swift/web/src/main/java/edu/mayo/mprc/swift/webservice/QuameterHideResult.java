package edu.mayo.mprc.swift.webservice;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.quameterdb.dao.QuameterAnnotation;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
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
public final class QuameterHideResult {
	@Resource(name = "quameterDao")
	private QuameterDao quameterDao;

	@RequestMapping(value = "/quameter-hide/{quameterResultId}", method = RequestMethod.POST)
	@ResponseBody
	public void hideQuameterResult(@PathVariable final int quameterResultId, @RequestParam final String reason) {
		quameterDao.begin();
		try {
			quameterDao.hideQuameterResult(quameterResultId, reason);
			quameterDao.commit();
		} catch (Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not hide quameter result " + quameterResultId, e);
		}
	}

	@RequestMapping(value = "/quameter-unhide/{quameterResultId}", method = RequestMethod.POST)
	public String unhideQuameterResult(@PathVariable final int quameterResultId, @RequestParam final String reason) {
		quameterDao.begin();
		try {
			quameterDao.unhideQuameterResult(quameterResultId, reason);
			quameterDao.commit();
		} catch (Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not un-hide quameter result " + quameterResultId, e);
		}
		return "redirect:/quameter/unhide.jsp";
	}

	@RequestMapping(value = "/new-annotation", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void createQuameterAnnotation(@RequestBody final MultiValueMap<String, String> inputMap) {
		System.out.println(inputMap.toString());
		quameterDao.begin();
		try {
			quameterDao.addAnnotation(new QuameterAnnotation(inputMap.getFirst("metricCode"), Integer.parseInt(inputMap.getFirst("dbId")), inputMap.getFirst("text")));
			quameterDao.commit();
		} catch (Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not add quameter annotation.");
		}
	}


	@RequestMapping(value = "/list-annotation", method = RequestMethod.GET)
	public ModelAndView getAnnotationList() {
		quameterDao.begin();
		try {
			List<QuameterAnnotation> myList = quameterDao.listAnnotations();
			quameterDao.commit();
			ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("quameterannotation", myList);
			return modelAndView;
		} catch (Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could Not list Quameter Annotations for you.", e);
		}
	}


	//  http://localhost:8080/service/getQuameterDataTable
	@RequestMapping(value = "/getQuameterDataTable", method = RequestMethod.GET)
	public void getDataFile(HttpServletResponse response) {
		quameterDao.begin();
		try {
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=\"quameter.tsv\"");

			final List<QuameterProteinGroup> proteinGroups = quameterDao.listProteinGroups();
			final List<QuameterResult> quameterResults = quameterDao.listVisibleResults();
			try {
				final Writer bw = response.getWriter();
				writeHeader(bw, proteinGroups);
				writeRows(bw, quameterResults, proteinGroups);

			} catch (IOException e) {
				e.printStackTrace();
			}
			quameterDao.commit();
		} catch (Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not generate Quameter Data Table for file.", e);
		}
	}


	private void writeHeader(final Writer writer, final List<QuameterProteinGroup> protGrps) throws IOException {
		// Predefined column names
		List<String> myHeader = Lists.newArrayList("ID", "Start Time", "Path", "Duration (min)", "Category", "Search parameters ID", "Transaction ID");
		// Quameter Result Column names
		for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
			myHeader.add(QuameterResult.getColumnName(column));
		}
		// Quameter's Protein Group Column Names
		for (final QuameterProteinGroup proteinGroup : protGrps) {
			myHeader.add(proteinGroup.getName());
		}
		Joiner jn = Joiner.on("\t");       // Guava
		writer.write(jn.join(myHeader) + "\n");
	}

	private void writeRows(final Writer writer, final List<QuameterResult> results, final List<QuameterProteinGroup> proteinGroups) throws IOException {
		for (final QuameterResult result : results) {
			final TandemMassSpectrometrySample massSpecSample = result.getSearchResult().getMassSpecSample();
			final SearchEngineParameters parameters = result.getFileSearch().getSearchParameters();
			final Map<QuameterProteinGroup, Integer> identifiedSpectra = result.getIdentifiedSpectra();

			List<String> myRow = Lists.newArrayList(
					result.getId().toString(), // Id of the entry (for hiding)
					massSpecSample.getStartTime().toString(), // startTime
					massSpecSample.getFile().getAbsolutePath().toString(), // path
					Double.toString(massSpecSample.getRunTimeInSeconds() / 60.0), // duration
					result.getCategory().toString(),
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

			Joiner jn = Joiner.on("\t");      // Guava
			writer.write(jn.join(myRow) + "\n");
		}
	}


}
