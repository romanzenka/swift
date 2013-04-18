package edu.mayo.mprc.scaffold;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.daemon.CachableWorkPacket;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.WorkPacketBase;
import edu.mayo.mprc.scafml.ScafmlScaffold;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ScaffoldWorkPacket extends WorkPacketBase implements CachableWorkPacket {
	private static final long serialVersionUID = 20110407L;
	public static final String SF3 = ".sf3";
	private File outputFolder;
	private String experimentName;
	private ScafmlScaffold scafmlFile;
	private static final Logger LOGGER = Logger.getLogger(ScaffoldWorkPacket.class);

	public ScaffoldWorkPacket(final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);
	}

	public ScaffoldWorkPacket(final File outputFolder, final ScafmlScaffold scafmlFile, final String experimentName, final String taskId, final boolean fromScratch) {
		super(taskId, fromScratch);

		assert outputFolder != null : "Scaffold request cannot be created: Work folder was null";
		assert scafmlFile != null : "Scaffold request cannot be created: .scafml file was null";

		this.outputFolder = outputFolder;
		this.scafmlFile = scafmlFile;
		this.experimentName = experimentName;
	}

	public File getOutputFolder() {
		return outputFolder;
	}

	public ScafmlScaffold getScafmlFile() {
		return scafmlFile;
	}

	public String getExperimentName() {
		return experimentName;
	}

	@Override
	public void synchronizeFileTokensOnReceiver() {
		uploadAndWait("outputFolder");
	}

	@Override
	public boolean isPublishResultFiles() {
		return true;
	}

	@Override
	public File getOutputFile() {
		// This is not to be used anywhere but the cache. The cache uses
		// the output file to determine the parent folder for all outputs.
		return new File(outputFolder, experimentName + SF3);
	}

	@Override
	public String getStringDescriptionOfTask() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public WorkPacket translateToWorkInProgressPacket(File wipFolder) {
		// Not used as we do not actually run this through a cache
		return this;
	}

	@Override
	public List<String> getOutputFiles() {
		ArrayList<String> files = new ArrayList<String>(6);
		files.add(scafmlFile.getExperiment().getName() + SF3);
		files.addAll(scafmlFile.getExperiment().getExport().getExportFileList());
		return files;
	}

	@Override
	public boolean cacheIsStale(File subFolder, List<String> outputFiles) {
		long newestInput = scafmlFile.getNewestInputTime();

		long oldestOutput = Long.MAX_VALUE;
		for (final String file : outputFiles) {
			final File outputFile = new File(subFolder, file);
			if(!outputFile.exists() || !outputFile.isFile() || outputFile.length()<=0) {
				return true;
			}
			final long fileModified = outputFile.lastModified();
			if (fileModified < oldestOutput) {
				oldestOutput = fileModified;
				if (oldestOutput < newestInput) {
					return true;
				}
			}
		}

		String previousScafml = "";
		try {
			if (getScafmlFileLocation().exists() && getScafmlFileLocation().isFile()) {
				previousScafml = Files.toString(getScafmlFileLocation(), Charsets.UTF_8);
			}
		} catch (IOException e) {
			// SWALLOWED
			LOGGER.warn("Could not read previous .scafml file: " + getScafmlFileLocation().getAbsolutePath(), e);
		}

		return !previousScafml.equals(scafmlFile.getDocument());
	}

	public File getScafmlFileLocation() {
		return new File(outputFolder, experimentName + ".scafml");
	}

	@Override
	public void reportCachedResult(ProgressReporter reporter, File targetFolder, List<String> outputFiles) {
	}
}
