package edu.mayo.mprc.heme;

import edu.mayo.mprc.MprcException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Id of a protein - name
 * @author Roman Zenka
 */
public final class ProteinEntity {
	private final String accNum;
    private final String description;
	private final Double mass;
    private String sequence;
    private int totalSpectra;
    private Filter filter;
    private List<PeptideEntity> peptides;

    // Define Enum to Streaming Sort the collection of entities
    public enum Filter {
        MUTATION_CONFIRMED, UNSUPPORTED, RELATED_MUTANT, OTHER;
    };


	public ProteinEntity(String accNum, String description, Double massDelta, String seq) {
		this.accNum = accNum;
		this.description = description;
		this.mass = massDelta;
        this.sequence = seq;
        this.peptides = new ArrayList<PeptideEntity>(10);
	}

	public String getAccNum() {
		return accNum;
	}

	public String getDescription() {
		return description;
	}

    public String getNeatDescription(){
        String[] s = description.split(" ");
        String[] subset;
        if(mass == null){  // Non-mutant proteins don't have masses
            subset = Arrays.copyOfRange(s, 1, s.length);
        }
        else{
            subset = Arrays.copyOfRange(s, 1, s.length - 2);
        }
        return StringUtils.join(subset, " ");
    }

	public Double getMass() {
		return mass;
	}

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * This function sets the Filter value, based on priority. To prevent hierarchical over writing.
     */
    public void promoteFilter(Filter filter) {
        this.filter = filter;
    }

    public List<PeptideEntity> getPeptides() {
        return peptides;
    }

    public void appendPeptide(PeptideEntity p){
        peptides.add(p);
    }

    public int getTotalSpectra() {
        return totalSpectra;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public void setTotalSpectra(int totalSpectra) {
        if (this.totalSpectra != 0 && totalSpectra != this.totalSpectra) {
            throw new MprcException("Corrupted Scaffold report! ("+this.accNum+") spectra count changed: " + this.totalSpectra + " -> " + totalSpectra);
        }
        this.totalSpectra = totalSpectra;
    }

    public String getCigar(){
        String[] arr = this.description.split(" ");
        return arr[arr.length-2];
    }

}
