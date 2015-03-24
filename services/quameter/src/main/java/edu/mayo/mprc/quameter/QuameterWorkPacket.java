package edu.mayo.mprc.quameter;

import edu.mayo.mprc.daemon.WorkCache;
import edu.mayo.mprc.daemon.worker.CoreRequirements;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchengine.EngineWorkPacket;

import java.io.File;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class QuameterWorkPacket extends EngineWorkPacket implements CoreRequirements {
	private static final long serialVersionUID = 3243861715951582089L;
	public static final String[] QUAL_TXT_EXTENSION = new String[]{"qual.txt"};

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

	public QuameterWorkPacket(final boolean fromScratch) {
		super(fromScratch);
	}

	public QuameterWorkPacket(final boolean fromScratch, final File rawFile, final File idpDbFile, final boolean monoisotopic, final double fdrScoreCutoff, final File outputFile, final boolean publishResultFiles) {
		super(rawFile, outputFile, null, null, publishResultFiles, fromScratch);
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
	public File canonicalOutput(final File cacheFolder) {
		return new File(cacheFolder, WorkCache.getCanonicalOutput(getInputFile(), getOutputFile(), QUAL_TXT_EXTENSION));
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		return new QuameterWorkPacket(
				isFromScratch(),
				getInputFile(),
				getIdpDbFile(),
				isMonoisotopic(),
				getFdrScoreCutoff(),
				canonicalOutput(cacheFolder),
				isPublishResultFiles());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		final long outputFileModified = new File(subFolder, outputFiles.get(0)).lastModified();
		return getInputFile().lastModified() > outputFileModified ||
				getIdpDbFile().lastModified() > outputFileModified;
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
		return "QuameterWorkPacket{" +
				"rawFile=" + getInputFile() +
				", idpDbFile=" + idpDbFile +
				", monoisotopic=" + monoisotopic +
				", fdrScoreCutoff=" + fdrScoreCutoff +
				", outputFile=" + getOutputFile() +
				'}';
	}

	@Override
	public int getNumRequiredCores() {
		return 8;
	}
}
