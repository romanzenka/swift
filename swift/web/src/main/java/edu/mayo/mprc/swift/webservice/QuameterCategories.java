package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.quameterdb.QuameterUi;
import edu.mayo.mprc.swift.ui.client.widgets.QuameterConfigurationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Controller
public final class QuameterCategories {
	@Resource(name = "swiftEnvironment")
	private RunningApplicationContext runningApplicationContext;

	@RequestMapping(value = "/quameter-categories", method = RequestMethod.GET)
	public ModelAndView listParameterSets() {
		try {
			final QuameterUi.Config config = getRunningApplicationContext().getSingletonConfig(QuameterUi.Config.class);
			final LinkedHashMap<String, String> categoryMap = QuameterConfigurationUtils.parseCategories(config.getQuameterConfig().getCategories());

			final ArrayList<QuameterCategory> result = new ArrayList<QuameterCategory>(categoryMap.size());
			for (final Map.Entry<String, String> category : categoryMap.entrySet()) {
				result.add(new QuameterCategory(category.getValue(), category.getKey()));
			}

			final ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("categories", result);
			return modelAndView;
		} catch (Exception t) {
			throw new MprcException("Could not list QuaMeter categories", t);
		}
	}

	public RunningApplicationContext getRunningApplicationContext() {
		return runningApplicationContext;
	}

	public void setRunningApplicationContext(final RunningApplicationContext context) {
		this.runningApplicationContext = context;
	}
}
