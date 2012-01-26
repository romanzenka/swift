package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.PersistableBase;

import java.util.List;

/**
 * A list of search results for a particular tandem mass spectrometry sample.
 * <p/>
 * This Scaffold spectrum report field is being parsed when creating this object:
 * <ul>
 * <li>MS/MS sample name - used to link to {@link TandemMassSpectrometrySample} with more information</li>
 * </ul>
 *
 * @author Roman Zenka
 */
public class SearchResult extends PersistableBase {
    /**
     * The mass spec sample.
     */
    private TandemMassSpectrometrySample massSpecSample;

    /**
     * List of all protein groups identified in this sample.
     */
    private List<ProteinGroup> proteinGroups;

    /**
     * Empty constructor for Hibernate.
     */
    public SearchResult() {
    }

    public SearchResult(TandemMassSpectrometrySample massSpecSample, List<ProteinGroup> proteinGroups) {
        this.massSpecSample = massSpecSample;
        this.proteinGroups = proteinGroups;
    }

    public TandemMassSpectrometrySample getMassSpecSample() {
        return massSpecSample;
    }

    public void setMassSpecSample(TandemMassSpectrometrySample massSpecSample) {
        this.massSpecSample = massSpecSample;
    }

    public List<ProteinGroup> getProteinGroups() {
        return proteinGroups;
    }

    public void setProteinGroups(List<ProteinGroup> proteinGroups) {
        this.proteinGroups = proteinGroups;
    }
}
