package edu.mayo.mprc.swift.controller;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.quameterdb.QuameterUi;
import edu.mayo.mprc.quameterdb.dao.QuameterAnnotation;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.utilities.StringUtilities;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Controller
public final class QuaMeter {
	private SwiftEnvironment environment;

	@RequestMapping(value = "/quameter", method = RequestMethod.GET)
	public final String quameter(final ModelMap model) {

		final QuameterUi quameterUi = getQuameterUi();
		model.addAttribute("quameterUi", quameterUi);
		model.addAttribute("daemonName", environment.getDaemonConfig().getName());

		if (quameterUi != null) {
			final StringWriter writer = new StringWriter(10000);
			quameterUi.writeMetricsJson(writer);
			model.addAttribute("metricsJson", writer.toString());
		} else {
			model.addAttribute("metricsJson", "null");
		}

		if (quameterUi != null) {
			quameterUi.begin();
			try {
				final StringWriter writer = new StringWriter(10000);
				quameterUi.dataTableJson(writer);
				quameterUi.commit();
				model.addAttribute("dataJson", writer.toString());
			} catch (final Exception e) {
				quameterUi.rollback();
				throw new MprcException(e);
			}
		} else {
			model.addAttribute("dataJson", "null");
		}

		return "quameter/index";
	}

	@RequestMapping(value = "/quameter/tags", method = RequestMethod.GET)
	public final String tags(final ModelMap model) {
		final QuameterUi quameterUi = getQuameterUi();
		if (quameterUi != null) {
			quameterUi.begin();
			try {
				final List<QuameterAnnotation> myList = quameterUi.getQuameterDao().listAnnotations();
				final Collection<QuameterTag> quameterTags = new ArrayList<QuameterTag>(myList.size());

				for (final QuameterAnnotation qa : myList) {
					final String metricName = QuameterUi.getMetricName(qa.getMetricCode());
					if (metricName != null) {
						final File file = qa.getQuameterResult().getSearchResult().getMassSpecSample().getFile();
						final QuameterTag quameterTag = new QuameterTag(
								StringUtilities.getDirectoryString(file),
								file.getName(),
								quameterUi.mapInstrument(qa.getQuameterResult().getSearchResult().getMassSpecSample().getInstrumentSerialNumber()),
								metricName,
								qa.getText());
						quameterTags.add(quameterTag);
					}
				}
				quameterUi.commit();

				model.addAttribute("tags", quameterTags);
			} catch (final Exception e) {
				quameterUi.rollback();
				throw new MprcException(e);
			}
		}
		return "quameter/tags";
	}

	@RequestMapping(value = "/quameter/unhide", method = RequestMethod.GET)
	public final String unhide(final ModelMap model) {
		final QuameterUi quameterUi = getQuameterUi();
		if (quameterUi != null) {
			quameterUi.begin();
			try {
				final List<QuameterResult> myList = quameterUi.getQuameterDao().listHiddenResults();
				final List<QuameterUnhide> unhides = Lists.newArrayListWithExpectedSize(myList.size());

				for (final QuameterResult qr : myList) {
					final File file = qr.getSearchResult().getMassSpecSample().getFile().getAbsoluteFile();
					final QuameterUnhide unhide = new QuameterUnhide(
							StringUtilities.getDirectoryString(file),
							StringUtilities.getFileNameString(file),
							qr.getId(),
							qr.getHiddenReason());
					unhides.add(unhide);
				}
				quameterUi.commit();

				model.addAttribute("unhides", unhides);
			} catch (final Exception e) {
				quameterUi.rollback();
				throw new MprcException(e);
			}
		}

		return "quameter/unhide";
	}


	private QuameterUi getQuameterUi() {
		final ResourceConfig quameterUiConfig = environment.getSingletonConfig(QuameterUi.Config.class);
		final QuameterUi quameterUi;
		if (quameterUiConfig != null) {
			quameterUi = (QuameterUi) environment.createResource(quameterUiConfig);
		} else {
			quameterUi = null;
		}
		return quameterUi;
	}

	public SwiftEnvironment getEnvironment() {
		return environment;
	}

	@Resource(name = "swiftEnvironment")
	public void setEnvironment(final SwiftEnvironment environment) {
		this.environment = environment;
	}
}
