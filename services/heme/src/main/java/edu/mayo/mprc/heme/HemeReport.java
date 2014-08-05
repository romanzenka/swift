package edu.mayo.mprc.heme;

import edu.mayo.mprc.heme.dao.HemeTest;

import java.util.*;

/**
 * @author Roman Zenka
 */
public final class HemeReport {
	private String name;
	private Date date;
	private double mass;
	private double massTolerance;
    private HashMap<String, ProteinEntity> allmyProteins;


    public HemeReport(final HemeTest hemeTest) {
		this.name = hemeTest.getName();
		this.date = hemeTest.getDate();
		this.mass = hemeTest.getMass();
		this.massTolerance = hemeTest.getMassTolerance();
        this.allmyProteins = new HashMap<String, ProteinEntity>(100);
	}

	public String getName() {
		return name;
	}

	public Date getDate() {
		return date;
	}

	public double getMass() {
		return mass;
	}

	public double getMassTolerance() {
		return massTolerance;
	}

	public boolean isMatch(final ProteinEntity id) {
		return id.getMass() != null && Math.abs(id.getMass() - getMass()) <= getMassTolerance();
	}

    // Keeps a collection of proteins in HemeReport -> return it if exists...create a new one if it doesn't
    public ProteinEntity find_or_create_ProteinEntity(String accNum, String description, Double massIsotopic, String seq) {
        ProteinEntity thisPE = allmyProteins.get(accNum);
        if( thisPE == null ){
            thisPE = new ProteinEntity(accNum,description,massIsotopic,seq);
            allmyProteins.put(accNum,thisPE);
        }
        return thisPE;
    }

    public List<ProteinEntity> get_ProteinEntities_by_filter( ProteinEntity.Filter f){
        List<ProteinEntity> pe = new ArrayList<ProteinEntity>();
        for (ProteinEntity value : allmyProteins.values()) {
            if( f.equals(value.getFilter()) ){
                pe.add(value);
            }
        }
        return pe;
    }

}
