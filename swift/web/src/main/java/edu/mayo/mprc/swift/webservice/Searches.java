package edu.mayo.mprc.swift.webservice;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.ui.server.SearchInput;
import edu.mayo.mprc.swift.ui.server.ServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
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
public final class Searches {
	@Resource(name = "SwiftAppService")
	private ServiceImpl swiftService;

	public Searches() {
	}

	@RequestMapping(value = "/searches", method = RequestMethod.POST)
	public ModelAndView runSearch(
			@RequestBody final MultiValueMap<String, String> searchInputMap) {
		try {
			final SearchInput searchInput = new SearchInput(searchInputMap);
			final long searchRunId = swiftService.startNewSearch(searchInput);
			final Search result = new Search(searchRunId, searchInput.getTitle());
			final Collection<Search> searches = new ArrayList<Search>(1);
			searches.add(result);

			final ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject(searches);
			return modelAndView;
		} catch (Exception t) {
			// SWALLOWED - we return the exception as an object
			final ModelAndView modelAndView = new ModelAndView();
			final Collection<SearchError> errors = new ArrayList<SearchError>();
			errors.add(new SearchError(MprcException.getDetailedMessage(t)));
			modelAndView.addObject(errors);
			return modelAndView;
		}
	}

}
