package edu.mayo.mprc.quameter;

import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class QuaMeterWorkPacket extends EngineWorkPacket {
	private static final long serialVersionUID = 3243861715951582089L;

	/**
	 * Idpqonvert database corresponding to the input RAW file.
	 */
	private File idpDbFile;

	/**
	 * When true, monoisotopic masses will be used. When false, average masses (ltq instrument)
	 */
	private boolean monoisotopic;

	/**
	 * Cutoff FDR score for IDs (0.05 == 5%)
	 */
	private double fdrScoreCutoff;

	public QuaMeterWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public QuaMeterWorkPacket(final String taskId, final boolean fromScratch, final File rawFile, final File idpDbFile, final boolean monoisotopic, final double fdrScoreCutoff, final File outputFile, final boolean publishResultFiles) {
		super(rawFile, outputFile, null, null, publishResultFiles, taskId, fromScratch);
		this.idpDbFile = idpDbFile;
		this.monoisotopic = monoisotopic;
		this.fdrScoreCutoff = fdrScoreCutoff;
	}

	@Override
	public boolean isPublishResultFiles() {
		return false;
	}

	@Override
	public String getStringDescriptionOfTask() {
		return "RAW:\n" + getInputFile().getAbsolutePath() + "\n\n" +
				"idpDB:\n" + getIdpDbFile().getAbsolutePath() + "\n\n" +
				"FDR cutoff:\n" + getFdrScoreCutoff() + "\n\n" +
				"Monoisotopic:\n" + isMonoisotopic() + "\n\n";
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new QuaMeterWorkPacket(getTaskId(), isFromScratch(),
				getInputFile(), getIdpDbFile(), isMonoisotopic(), getFdrScoreCutoff(), new File(wipFolder, getOutputFile().getName()), isPublishResultFiles());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long outputFileModified = new File(subFolder, outputFiles.get(0)).lastModified();
		return getInputFile().lastModified() > outputFileModified ||
				getIdpDbFile().lastModified() > outputFileModified;
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
	}

	public File getIdpDbFile() {
		return idpDbFile;
	}

	public boolean isMonoisotopic() {
		return monoisotopic;
	}

	public double getFdrScoreCutoff() {
		return fdrScoreCutoff;
	}

	@Override
	public String toString() {
		return "QuaMeterWorkPacket{" +
				"rawFile=" + getInputFile() +
				", idpDbFile=" + idpDbFile +
				", monoisotopic=" + monoisotopic +
				", fdrScoreCutoff=" + fdrScoreCutoff +
				", outputFile=" + getOutputFile() +
				'}';
	}
}
