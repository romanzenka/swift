package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.heme.HemeReport;
import edu.mayo.mprc.heme.HemeUi;
import edu.mayo.mprc.heme.ProteinEntity.Filter;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;

/**
 * @author Roman Zenka
 */
@Controller
public final class HemeService {
	private RunningApplicationContext applicationContext;
	private WebUiHolder webUiHolder;

	@RequestMapping(value = "/service/heme/data/{entry}/massDelta", method = RequestMethod.POST)
	public ModelAndView setMassDelta(
			@PathVariable
			final int entry,
			@RequestParam(value = "value", required = true)
			final double value) {
		final HemeUi hemeUi = getHemeUi();

		final ModelAndView modelAndView = new ModelAndView();

		hemeUi.begin();
		try {
			hemeUi.setMassDelta(entry, value);
			hemeUi.commit();
			modelAndView.addObject("value", value);
		} catch (Exception e) {
			hemeUi.rollback();
			throw new MprcException("Could not set mass delta", e);
		}

		return modelAndView;
	}

	@RequestMapping(value = "/service/heme/data/{entry}/massDeltaTolerance", method = RequestMethod.POST)
	public ModelAndView setMassDeltaTolerance(
			@PathVariable
			final int entry,
			@RequestParam(value = "value", required = true)
			final double value) {
		final HemeUi hemeUi = getHemeUi();

		final ModelAndView modelAndView = new ModelAndView();

		hemeUi.begin();
		try {
			hemeUi.setMassDeltaTolerance(entry, value);
			hemeUi.commit();
			modelAndView.addObject("value", value);
		} catch (Exception e) {
			hemeUi.rollback();
			throw new MprcException("Could not set mass delta tolerance", e);
		}

		return modelAndView;
	}

	@RequestMapping(value = "/service/heme/data/{entry}/startSearch", method = RequestMethod.POST)
	public ModelAndView startSearch(@PathVariable final int entry) {
		final HemeUi hemeUi = getHemeUi();

		final ModelAndView modelAndView = new ModelAndView();

		try {
			final long searchId = hemeUi.startSwiftSearch(entry);
			modelAndView.addObject("searchId", searchId);
		} catch (Exception e) {
			throw new MprcException("Could not start Swift search", e);
		}

		return modelAndView;
	}

	//GET from HemePath list when button is "Result" -> to the report.
	@RequestMapping(value = "/service/heme/data/{entry}/report.*", method = RequestMethod.GET)
	public ModelAndView viewReport(@PathVariable final int entry) {
		final HemeUi hemeUi = getHemeUi();
		final ModelAndView modelAndView = new ModelAndView();

		modelAndView.addObject("title", getWebUi().getTitle());

		hemeUi.begin();
		try {
			HemeReport report = hemeUi.createReport(entry);
			modelAndView.setViewName("heme/heme_report"); //Migrated to JSP
			modelAndView.addObject("report", report);
			final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			modelAndView.addObject("confirmedList", report.get_ProteinEntities_by_filter(Filter.MUTATION_CONFIRMED));
			modelAndView.addObject("relatedList", report.get_ProteinEntities_by_filter(Filter.RELATED_MUTANT));
			modelAndView.addObject("unsupportedList", report.get_ProteinEntities_by_filter(Filter.UNSUPPORTED));
			modelAndView.addObject("otherList", report.get_ProteinEntities_by_filter(Filter.OTHER));
			modelAndView.addObject("reportDate", format.format(report.getDate()));

			hemeUi.commit();
		} catch (Exception e) {
			hemeUi.rollback();
			throw new MprcException("Could not view report for entry " + entry, e);
		}

		return modelAndView;

	}

	private HemeUi getHemeUi() {
		return (HemeUi) getApplicationContext().createResource(getApplicationContext().getSingletonConfig(HemeUi.Config.class));
	}

	public RunningApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Resource(name = "swiftEnvironment")
	public void setApplicationContext(final RunningApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}
