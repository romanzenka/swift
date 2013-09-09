package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.heme.HemeUi;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * @author Roman Zenka
 */
@Controller
public final class HemeService {
	private RunningApplicationContext applicationContext;

	@RequestMapping(value = "/heme/data/{entry}/massDelta", method = RequestMethod.POST)
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

	@RequestMapping(value = "/heme/data/{entry}/massDeltaTolerance", method = RequestMethod.POST)
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

	@RequestMapping(value = "/heme/data/{entry}/startSearch", method = RequestMethod.POST)
	public ModelAndView startSearch(@PathVariable final int entry) {
		final HemeUi hemeUi = getHemeUi();

		final ModelAndView modelAndView = new ModelAndView();

		hemeUi.begin();
		try {
			final long searchId = hemeUi.startSwiftSearch(entry);
			modelAndView.addObject("searchId", searchId);
			hemeUi.commit();
		} catch (Exception e) {
			hemeUi.rollback();
			throw new MprcException("Could not start Swift search", e);
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
}
