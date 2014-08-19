package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.quameterdb.dao.QuameterAnnotation;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Controller
public final class QuameterHideResult {
	@Resource(name = "quameterDao")
	private QuameterDao quameterDao;

	@RequestMapping(value = "/quameter-hide/{quameterResultId}", method = RequestMethod.POST)
	@ResponseBody
	public void hideQuameterResult(@PathVariable final int quameterResultId) {
		quameterDao.begin();
		try {
			quameterDao.hideQuameterResult(quameterResultId);
			quameterDao.commit();
		} catch (Exception e) {
			quameterDao.rollback();
			throw new MprcException("Could not hide quameter result " + quameterResultId, e);
		}

	}



    @RequestMapping(value = "/new-annotation", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createQuameterAnnotation( @RequestBody final MultiValueMap<String, String> inputMap ) {
        System.out.println(inputMap.toString());
        quameterDao.begin();
        try {
            quameterDao.addAnnotation(new QuameterAnnotation(inputMap.getFirst("metricCode"), Integer.parseInt(inputMap.getFirst("dbId")), inputMap.getFirst("text")));
            quameterDao.commit();
        } catch (Exception e) {
            quameterDao.rollback();
            throw new MprcException("Could not add quameter annotation.");
        }
    }


    @RequestMapping(value = "/list-annotation", method = RequestMethod.GET)
    public ModelAndView getAnnotationList(){
        quameterDao.begin();
        try {
            List<QuameterAnnotation> myList = quameterDao.listAnnotations();
            quameterDao.commit();
            ModelAndView modelAndView=new ModelAndView();
            modelAndView.addObject("quameterannotation", myList);
            return modelAndView;
        } catch (Exception e) {
            quameterDao.rollback();
            throw new MprcException("Could Not list Quameter Annotations for you.");
        }
    }


}
