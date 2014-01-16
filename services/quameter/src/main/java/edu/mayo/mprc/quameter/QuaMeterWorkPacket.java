package edu.mayo.mprc.quameter;

import com.google.common.collect.Lists;
import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class QuaMeterWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 3243861715951582089L;

	/**
	 * The .RAW file we are measuring the quality of.
	 */
	private File rawFile;

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

	/**
	 * The resulting quality metrics file.
	 */
	private File outputFile;

	public QuaMeterWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public QuaMeterWorkPacket(final String taskId, final boolean fromScratch, final File rawFile, final File idpDbFile, final boolean monoisotopic, final double fdrScoreCutoff, final File outputFile) {
		super(taskId, fromScratch);
		this.rawFile = rawFile;
		this.idpDbFile = idpDbFile;
		this.monoisotopic = monoisotopic;
		this.fdrScoreCutoff = fdrScoreCutoff;
		this.outputFile = outputFile;
	}

	@Override
	public boolean isPublishResultFiles() {
		return false;
	}

	@Override
	public File getOutputFile() {
		return outputFile;
	}

	@Override
	public String getStringDescriptionOfTask() {
		return "RAW:\n" + getRawFile().getAbsolutePath() + "\n\n" +
				"idpDB:\n" + getIdpDbFile().getAbsolutePath() + "\n\n" +
				"FDR cutoff:\n" + getFdrScoreCutoff() + "\n\n" +
				"Monoisotopic:\n" + isMonoisotopic() + "\n\n";
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
		return new QuaMeterWorkPacket(getTaskId(), isFromScratch(),
				getRawFile(), getIdpDbFile(), isMonoisotopic(), getFdrScoreCutoff(), new File(wipFolder, getOutputFile().getName()));
	}

	@Override
	public List<String> getOutputFiles() {
		return Lists.newArrayList(getOutputFile().getName());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long outputFileModified = new File(subFolder, outputFiles.get(0)).lastModified();
		return getRawFile().lastModified() > outputFileModified ||
				getIdpDbFile().lastModified() > outputFileModified;
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
	}

	public File getRawFile() {
		return rawFile;
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
				"rawFile=" + rawFile +
				", idpDbFile=" + idpDbFile +
				", monoisotopic=" + monoisotopic +
				", fdrScoreCutoff=" + fdrScoreCutoff +
				", outputFile=" + outputFile +
				'}';
	}
}
