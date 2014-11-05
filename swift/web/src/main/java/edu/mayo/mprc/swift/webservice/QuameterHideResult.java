package edu.mayo.mprc.swift.webservice;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.quameterdb.dao.QuameterAnnotation;
import edu.mayo.mprc.quameterdb.dao.QuameterDao;
import edu.mayo.mprc.quameterdb.dao.QuameterProteinGroup;
import edu.mayo.mprc.quameterdb.dao.QuameterResult;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @RequestMapping(value = "/quameter-unhide/{quameterResultId}", method = RequestMethod.POST)
    public String unhideQuameterResult(@PathVariable final int quameterResultId) {
        quameterDao.begin();
        try {
            quameterDao.unhideQuameterResult(quameterResultId);
            quameterDao.commit();
        } catch (Exception e) {
            quameterDao.rollback();
            throw new MprcException("Could not un-hide quameter result " + quameterResultId, e);
        }
	    return "redirect:/quameter/unhide.jsp";
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


      //  http://localhost:8080/service/getQuameterDataTable
    @RequestMapping(value = "/getQuameterDataTable", method = RequestMethod.GET)
    public FileSystemResource getDataFile(){
        quameterDao.begin();
        try{
            final List<QuameterProteinGroup> proteinGroups = quameterDao.listProteinGroups();
            final List<QuameterResult> quameterResults = quameterDao.listVisibleResults();
            quameterDao.commit();

            try{
                //create a temp file
                File temp = File.createTempFile("QuameterDataTable", ".tsv");
                //write it
                BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
                writeHeader(bw, proteinGroups);
                writeRows(bw, quameterResults, proteinGroups);
                bw.close();

                return new FileSystemResource(temp);
            }catch(IOException e){
                e.printStackTrace();
            }
        }catch (Exception e) {
            quameterDao.rollback();
            throw new MprcException("Could not generate Quameter Data Table for file.");
        }

        return null;
    }


    private void writeHeader(final BufferedWriter writer, final List<QuameterProteinGroup> protGrps) throws IOException{
        // Predefined column names
        List<String> myHeader = Arrays.asList("ID", "Start Time", "Path", "Duration (min)", "Instrument", "Category", "Search parameters ID", "Transaction ID");
        // Quameter Result Column names
        for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
            myHeader.add( QuameterResult.getColumnName(column) );
        }
        // Quameter's Protein Group Column Names
        for (final QuameterProteinGroup proteinGroup : protGrps) {
            myHeader.add( proteinGroup.getName() );
        }
        Joiner jn = Joiner.on(" ");
        writer.write( jn.join(myHeader) );
    }

    private void writeRows(final BufferedWriter writer, final List<QuameterResult> results, final List<QuameterProteinGroup> proteinGroups) throws IOException {
       for (final QuameterResult result : results) {
           final TandemMassSpectrometrySample massSpecSample = result.getSearchResult().getMassSpecSample();
           final SearchEngineParameters parameters = result.getFileSearch().getSearchParameters();
           final Map<QuameterProteinGroup, Integer> identifiedSpectra = result.getIdentifiedSpectra();

           List<String> myRow = Arrays.asList(
                result.getId().toString(), // Id of the entry (for hiding)
                massSpecSample.getStartTime().toString(), // startTime
                massSpecSample.getFile().getAbsolutePath().toString(), // path
                Double.toString( massSpecSample.getRunTimeInSeconds() / 60.0 ), // duration
                //   mapInstrument(massSpecSample.getInstrumentSerialNumber()), // instrument
                massSpecSample.getInstrumentSerialNumber().toString(), // instrument internal id - may remove all together
                result.getCategory().toString(),
                Integer.toString( parameters != null ? parameters.getId() : 0 ), // search parameters id
                Integer.toString( result.getTransaction() )
           );

           for (final QuameterResult.QuameterColumn column : QuameterResult.QuameterColumn.values()) {
               myRow.add( Double.toString(result.getValue(column)) );
           }

           for (final QuameterProteinGroup proteinGroup : proteinGroups) {
               final Integer numSpectra = identifiedSpectra.get(proteinGroup);
               myRow.add( Integer.toString( numSpectra != null ? numSpectra : 0) );
           }

           Joiner jn = Joiner.on(" ");
           writer.write( jn.join(myRow) );
       }
    }


}
