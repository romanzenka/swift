package edu.mayo.mprc.heme;

import edu.mayo.mprc.MprcException;

import java.util.List;

/**
 * @author Roman Zenka
 */
public final class HemeReportEntry {
	private List<ProteinId> proteinIds;
	private int totalSpectra;

	public HemeReportEntry(List<ProteinId> proteinIds, int totalSpectra) {
		this.proteinIds = proteinIds;
		this.totalSpectra = totalSpectra;
	}

	public List<ProteinId> getProteinIds() {
		return proteinIds;
	}

	public void setProteinIds(List<ProteinId> proteinIds) {
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
