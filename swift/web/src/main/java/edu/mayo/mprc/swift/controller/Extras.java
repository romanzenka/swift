package edu.mayo.mprc.swift.controller;

import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * Data for extras
 *
 * @author Roman Zenka
 */
@Controller
@RequestMapping("/extras")
public final class Extras {
	@Resource(name = "webUiHolder")
	private WebUiHolder webUiHolder;

	@RequestMapping(method = RequestMethod.GET)
	public String extras(final ModelMap model) {
		// Dates for report
		DateTime end = new DateTime();
		DateTime start = end.minusMonths(1);

		model.addAttribute("startDate", DateTimeFormat.forPattern("yyyy-MM-dd").print(start));
		model.addAttribute("endDate", DateTimeFormat.forPattern("yyyy-MM-dd").print(end));

		model.addAttribute("buildVersion", ReleaseInfoCore.buildVersion());
		model.addAttribute("buildRevision", ReleaseInfoCore.buildRevision());
		model.addAttribute("buildTimestamp", ReleaseInfoCore.buildTimestamp());
		model.addAttribute("buildLink", ReleaseInfoCore.buildLink());

		final Period period = new Interval(webUiHolder.getStartTime(), new DateTime()).toPeriod();
		PeriodFormatter formatter = new PeriodFormatterBuilder()
				.appendDays()
				.appendSuffix(" days, ")
				.appendHours()
				.appendSuffix(" hours, ")
				.appendMinutes()
				.appendSuffix(" minutes, ")
				.appendSeconds()
				.appendSuffix(" seconds.")
				.toFormatter();

		model.addAttribute("uptime", formatter.print(period));

		return "extras";
	}
}

