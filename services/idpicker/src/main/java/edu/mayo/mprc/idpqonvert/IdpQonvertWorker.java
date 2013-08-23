package edu.mayo.mprc.idpqonvert;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls <tt>msaccess.exe</tt> to determine whether peak picking should be enabled.
 * Then calls <tt>msconvert.exe</tt>.
 */
public final class IdpQonvertWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(IdpQonvertWorker.class);
	public static final String TYPE = "idpqonvert";
	public static final String NAME = "IdpQonvert";
	public static final String DESC = "<p>IdpQonvert uses machine learning algorithms to separate correct and incorrect peptide spectrum matches.</p>" +
			"<p>Inputs are results from the search engines (in .mzid format), output is an .idp file (sqlite3) with search engine scores " +
			"recalculated to match a particular target FDR.</p>";

	private static final String IDPQONVERT_EXECUTABLE = "idpqonvert";

	private File idpQonvertExecutable;

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof IdpQonvertWorkPacket)) {
			ExceptionUtilities.throwCastException(workPacket, IdpQonvertWorkPacket.class);
			return;
		}

		final IdpQonvertWorkPacket batchWorkPacket = (IdpQonvertWorkPacket) workPacket;

		LOGGER.debug("Running IdpQonvert: [" + batchWorkPacket.getInputFile().getAbsolutePath() + "] -> " + batchWorkPacket.getOutputFile());

		//  check if already exists (skip condition)
		if (isConversionDone(batchWorkPacket)) {
			return;
		}

		// Make sure the database is set properly
		batchWorkPacket.getParams().setProteinDatabase(batchWorkPacket.getFastaFile().getAbsolutePath());
		final List<String> commandLine = new ArrayList<String>();
		commandLine.add(FileUtilities.getAbsoluteFileForExecutables(getIdpQonvertExecutable()).getPath());
		commandLine.add("-cpus");
		commandLine.add(String.valueOf(getNumThreads()));
		commandLine.add("-workdir");
		commandLine.add(batchWorkPacket.getInputFile().getParentFile().getAbsolutePath());
		commandLine.addAll(batchWorkPacket.getParams().toCommandLine());
		commandLine.add(batchWorkPacket.getInputFile().getName());
		LOGGER.info("Running idpQonvert:\n\t" + Joiner.on("\n\t").join(commandLine).toString());

		final ProcessBuilder builder = new ProcessBuilder(commandLine);
		builder.directory(idpQonvertExecutable.getParentFile());
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.runAndCheck("idpQonvert");
		final File from = new File(batchWorkPacket.getInputFile().getParentFile(),
				FileUtilities.stripExtension(batchWorkPacket.getInputFile().getName()) + ".idpDB");

		try {
			FileUtilities.ensureFolderExists(batchWorkPacket.getOutputFile().getParentFile());
			Files.move(
					from,
					batchWorkPacket.getOutputFile());
		} catch (Exception e) {
			throw new MprcException("Failed to move the resulting file from [" + from.getAbsolutePath() + "] to [" + batchWorkPacket.getOutputFile() + "]", e);
		}
		if (!batchWorkPacket.getOutputFile().exists() || !batchWorkPacket.getOutputFile().canRead() || !batchWorkPacket.getOutputFile().isFile()) {
			throw new MprcException("idpQonvert failed to create file: " + batchWorkPacket.getOutputFile().getAbsolutePath());
		}
	}

	private static int getNumThreads() {
		return Math.max(1, Runtime.getRuntime().availableProcessors());
	}

	private static boolean isConversionDone(final IdpQonvertWorkPacket batchWorkPacket) {
		final File resultFile = batchWorkPacket.getOutputFile();
		if (resultFile.exists()) {
			final long resultModified = resultFile.lastModified();
			final File inputFile = batchWorkPacket.getInputFile();
			if (inputFile.lastModified() > resultModified) {
				LOGGER.info("The input file [" + inputFile.getAbsolutePath() + "] is newer than [" + resultFile.getAbsolutePath() + "]");
				return false;

			}
			LOGGER.info(resultFile.getAbsolutePath() + " already exists and sufficiently recent.");
			return true;
		}
		return false;
	}

	public String toString() {
		return MessageFormat.format("IdpQonvert:\n\tidpQonvert={0}\n", getIdpQonvertExecutable().getPath());
	}

	public File getIdpQonvertExecutable() {
		return idpQonvertExecutable;
	}

	public void setIdpQonvertExecutable(final File idpQonvertExecutable) {
		this.idpQonvertExecutable = idpQonvertExecutable;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("idpQonvertWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory {
		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"IDPQONVERT", ".idp", "IdpQonvert", false, "idpqonvert", null,
				new String[]{TYPE},
				new String[]{IdpQonvertCache.TYPE},
				new String[]{},
				80, true);

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final IdpQonvertWorker worker = new IdpQonvertWorker();
			worker.setIdpQonvertExecutable(new File(config.getIdpQonvertExecutable()));
			return worker;
		}

		@Override
		public EngineMetadata getEngineMetadata() {
			return ENGINE_METADATA;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {

		private String idpQonvertExecutable;

		public Config() {
		}

		public Config(final String idpQonvertExecutable) {
			setIdpQonvertExecutable(idpQonvertExecutable);
		}

		public String getIdpQonvertExecutable() {
			return idpQonvertExecutable;
		}

		public void setIdpQonvertExecutable(final String idpQonvertExecutable) {
			this.idpQonvertExecutable = idpQonvertExecutable;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(IDPQONVERT_EXECUTABLE, getIdpQonvertExecutable());
		}

		@Override
		public void load(final ConfigReader reader) {
			setIdpQonvertExecutable(reader.get(IDPQONVERT_EXECUTABLE));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder

					.property(IDPQONVERT_EXECUTABLE, "<tt>idpQonvert</tt> path", "Location of IdpQonvert ver. 3 <tt>idpQonvert</tt>." +
							"<p><a href=\"http://teamcity.fenchurch.mc.vanderbilt.edu/project.html?projectId=project9&tab=projectOverview\">TeamCity download from Vanderbilt</a></p>")
					.required()
					.executable(Lists.<String>newArrayList("-dump"))
					.defaultValue("idpQonvert");
		}
	}
}
