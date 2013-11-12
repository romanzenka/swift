package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Roman Zenka
 */
@Controller
public final class Engines {
	private WebUiHolder webUiHolder;

	public Engines() {
	}

	@RequestMapping(value = "/engines", method = RequestMethod.GET)
	public ModelAndView listSearchEngines() {
		try {
			final Collection<SearchEngine> searchEngines = getWebUiHolder().getWebUi().getSearchEngines();
			final ArrayList<Engine> result =
					new ArrayList<Engine>(searchEngines.size());
			for (final SearchEngine searchEngine : searchEngines) {
				result.add(new Engine(searchEngine));
			}

			ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("search-engines", result);
			return modelAndView;
		} catch (Exception t) {
			throw new MprcException("Could not list users", t);
		}
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}
}
