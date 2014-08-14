package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.quameterdb.dao.QuameterAnnotation;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

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



    @RequestMapping(value = "/new-annotation/{form}", method = RequestMethod.POST)
    @ResponseBody
    public void createQuameterAnnotation(QuameterAnnotation input) {
       System.out.println(input);
       /* quameterDao.begin();
        try {
            quameterDao.hideQuameterResult(quameterResultId);
            quameterDao.commit();
        } catch (Exception e) {
            quameterDao.rollback();
            throw new MprcException("Could not hide quameter result " + quameterResultId, e);
        }*/

    }
    //quameterDao.listAnnotations() // use the interface to access



}
