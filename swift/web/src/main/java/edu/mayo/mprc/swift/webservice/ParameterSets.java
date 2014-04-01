package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.SavedSearchEngineParameters;
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
public final class ParameterSets {
	@Resource(name = "paramsDao")
	private ParamsDao paramsDao;

	@RequestMapping(value = "/parameter-sets", method = RequestMethod.GET)
	public ModelAndView listParameterSets() {
		try {
			getParamsDao().begin();
			final List<SavedSearchEngineParameters> params = getParamsDao().savedSearchEngineParameters();
			final ArrayList<ParameterSet> result =
					new ArrayList<ParameterSet>(params.size());
			for (final SavedSearchEngineParameters parameterSet : params) {
				result.add(new ParameterSet(parameterSet));
			}

			getParamsDao().commit();
			ModelAndView modelAndView = new ModelAndView();
			modelAndView.addObject("params", result);
			return modelAndView;
		} catch (Exception t) {
			getParamsDao().rollback();
			throw new MprcException("Could not list parameter sets", t);
		}
	}

	public ParamsDao getParamsDao() {
		return paramsDao;
	}

	public void setParamsDao(ParamsDao paramsDao) {
		this.paramsDao = paramsDao;
	}
}
