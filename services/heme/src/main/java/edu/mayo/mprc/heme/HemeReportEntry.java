package edu.mayo.mprc.heme;

import edu.mayo.mprc.MprcException;

import java.util.List;

/**
 * @author Roman Zenka
 */
//TODO - DEPRICATE HemeReportEntry

public final class HemeReportEntry {
	private List<ProteinEntity> proteinIds;
	private int totalSpectra;
    private Filter filter;

    // Define Enum to Streaming Sort the collection of entities
    public enum Filter {
        MUTATION_CONFIRMED, MASS_IN_RANGE, OTHER;
    };


    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

	public HemeReportEntry(List<ProteinEntity> proteinIds, int totalSpectra) {
		this.proteinIds = proteinIds;
		this.totalSpectra = totalSpectra;
	}

	public List<ProteinEntity> getProteinIds() {
		return proteinIds;
	}

	public void setProteinIds(List<ProteinEntity> proteinIds) {
		this.proteinIds = proteinIds;
	}

	public void setTotalSpectra(int totalSpectra) {
		this.totalSpectra = totalSpectra;
	}

	public void checkSpectra(int spectra) {
		if (getTotalSpectra() != spectra) {
			throw new MprcException("Corrupted Scaffold report. Protein spectrum count changed from + " + getTotalSpectra() + " to " + spectra + " for accnum " + getProteinIds().get(0).getAccNum());
		}
	}

	public int getTotalSpectra() {
		return totalSpectra;
	}
}
