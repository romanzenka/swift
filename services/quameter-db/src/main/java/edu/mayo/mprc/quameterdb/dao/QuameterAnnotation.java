package edu.mayo.mprc.quameterdb.dao;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Raymond Moore
 */
public class QuameterAnnotation extends PersistableBase {
    private String metricCode;
    private Integer quameterResultId;
    private String text;


   // public QuameterAnnotation(){}

    @Autowired
    public QuameterAnnotation(String metricCode, Integer quameterResultId, String text) {
        this.metricCode = metricCode;
        this.quameterResultId = quameterResultId;
        this.text = text;
    }

    public QuameterAnnotation(String s) {
    }

    @Override
    public Criterion getEqualityCriteria() {
        throw new MprcException("EqualityCriteria Not Implemented");
    }

    public String prettyPrint(){
        return "Code: "+metricCode+" ResultId: "+quameterResultId+"\nText: "+text;
    }
}
