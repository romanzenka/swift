package edu.mayo.mprc.daemon;

import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.utilities.progress.ProgressReporter;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SimpleTestWorkPacket extends WorkPacketBase implements Serializable, CachableWorkPacket {
	private static final long serialVersionUID = -2096468611424782391L;
	private String name;
	private File resultFile;

	public SimpleTestWorkPacket(final String name, final boolean fromScratch) {
		super(fromScratch);
		this.name = name;
	}

	public File getResultFile() {
		return resultFile;
	}

	public void setResultFile(final File resultFile) {
		this.resultFile = resultFile;
	}

	@Override
	public boolean isPublishResultFiles() {
		return false;
	}

	@Override
	public File getOutputFile() {
		return resultFile;
	}

	@Override
	public String getStringDescriptionOfTask() {
		return getTaskId().toString();
	}

	@Override
	public WorkPacket translateToCachePacket(final File cacheFolder) {
		final SimpleTestWorkPacket translatedPacket = new SimpleTestWorkPacket("WIP:" + name, false);
		translatedPacket.setResultFile(new File(cacheFolder, getResultFile().getName()));
		return translatedPacket;
	}

	@Override
	public List<String> getOutputFiles() {
		return Arrays.asList(getResultFile().getName());
	}

	@Override
	public boolean cacheIsStale(final File subFolder, final List<String> outputFiles) {
		return false;
	}

	@Override
	public void reportCachedResult(final ProgressReporter reporter, final File targetFolder, final List<String> outputFiles) {
	}
}
