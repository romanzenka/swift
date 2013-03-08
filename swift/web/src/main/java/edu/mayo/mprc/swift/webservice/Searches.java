package edu.mayo.mprc.swift.webservice;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.ui.server.SearchInput;
import edu.mayo.mprc.swift.ui.server.ServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
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
			@ModelAttribute("searchInput") SearchInput searchInput) {
		try {
			final SwiftSearchDefinition search = swiftService.startNewSearch(searchInput);
			final Search result = new Search(search);
			final Collection<Search> searches = new ArrayList<Search>(1);
			searches.add(result);

			final ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("searches", searches);
			return modelAndView;
		} catch (Exception t) {
			throw new MprcException("Could not start Swift search", t);
		}
	}

}
