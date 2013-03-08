package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchEngineConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Controller
public final class Engines {
	@Resource(name = "swiftDao")
	private SwiftDao swiftDao;

	public Engines() {
	}

	@RequestMapping(value = "/engines", method = RequestMethod.GET)
	public ModelAndView listSearchEngines() {
		try {
			getSwiftDao().begin();
			final List<SearchEngineConfig> searchEngineConfigs = getSwiftDao().listSearchEngines();
			final ArrayList<Engine> result =
					new ArrayList<Engine>(searchEngineConfigs.size());
			for (final SearchEngineConfig searchEngineConfig : searchEngineConfigs) {
				result.add(new Engine(searchEngineConfig));
			}

			getSwiftDao().commit();
			ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("search-engines", result);
			return modelAndView;
		} catch (Exception t) {
			getSwiftDao().rollback();
			throw new MprcException("Could not list users", t);
		}
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}
}
