package edu.mayo.mprc.comet;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.msconvert.MsconvertCache;
import edu.mayo.mprc.msconvert.MsconvertResult;
import edu.mayo.mprc.msconvert.MsconvertWorkPacket;
import edu.mayo.mprc.msconvert.MsconvertWorker;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class CometWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(CometWorker.class);
	public static final String TYPE = "comet";
	public static final String NAME = "Comet";
	public static final String DESC = "Comet search engine support. <p>Comet is freely available at <a href=\"http://sourceforge.net/projects/comet-ms/\">http://sourceforge.net/projects/comet-ms/</a>.</p>";
	public static final String PEP_XML = ".pep.xml";
	public static final String SQT = ".sqt";
	public static final String MS2 = ".ms2";

	private File cometExecutable;

	public static final String COMET_EXECUTABLE = "cometExecutable";
	public static final String MSCONVERT = "msconvert";
	private DaemonConnection msconvertDaemon;

	public CometWorker(final File cometExecutable) {
		this.cometExecutable = cometExecutable;
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof CometWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + CometWorkPacket.class.getName());
		}

		final CometWorkPacket packet = (CometWorkPacket) workPacket;

		final File finalOutputFile = packet.getOutputFile();
		final File outputFile = getTempOutputFile(tempWorkFolder, finalOutputFile);
		final String resultFileName = getResultFileName(outputFile);
		final File parameterFile = makeParameterFile(tempWorkFolder, packet);

		final File finalMs2File;
		final MsconvertProgressListener listener;
		if (outputFile.getName().endsWith(SQT)) {
			// SQT files need .ms2 file available
			if (getMsconvertDaemon() == null) {
				throw new MprcException("Comet cannot produce .sqt results without configured msconvert worker");
			}
			String ms2FileName = FileUtilities.stripGzippedExtension(finalOutputFile.getName()) + MS2;
			finalMs2File = new File(finalOutputFile.getParentFile(), ms2FileName);
			final MsconvertWorkPacket msconvertWorkPacket =
					new MsconvertWorkPacket(finalMs2File, true, packet.getInputFile(), false, packet.isFromScratch(), false);
			listener = new MsconvertProgressListener(progressReporter, finalMs2File);
			LOGGER.info("Submitting msconvert work packet to " + getMsconvertDaemon().getConnectionName());
			getMsconvertDaemon().sendWork(msconvertWorkPacket, listener);
		} else {
			finalMs2File = null;
			listener = null;
		}

		final List<String> parameters = new LinkedList<String>();
		parameters.add(cometExecutable.getPath());
		parameters.add("-P" + parameterFile.getAbsolutePath()); // Parameter file
		parameters.add("-D" + packet.getDatabaseFile().getAbsolutePath()); // Database file
		parameters.add("-N" + resultFileName); // Result file
		parameters.add(packet.getInputFile().getAbsolutePath()); // Input file

		final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
		processBuilder.directory(tempWorkFolder);

		final ProcessCaller processCaller = new ProcessCaller(processBuilder, progressReporter.getLog());

		processCaller.run();

		if (processCaller.getExitValue() != 0) {
			throw new MprcException("Comet execution failed:\n" + processCaller.getFailedCallDescription());
		}

		if (listener != null) {
			try {
				listener.waitForFinished(0);
			} catch (InterruptedException e) {
				throw new MprcException("Waiting for MS2 conversion to complete was interrupted", e);
			}
		}

		publish(outputFile, finalOutputFile);
		if (finalMs2File != null) {
			publish(listener.getMs2File(), finalMs2File);
		}

		FileUtilities.quietDelete(parameterFile);

		LOGGER.info("Comet search, " + packet.toString() + ", has been successfully completed.");
	}

	/**
	 * Make parameter file for comet. This involves tweaking the input parameter string based on
	 * requested output file.
	 *
	 * @param tempWorkFolder
	 * @param packet
	 * @return
	 */
	private File makeParameterFile(final File tempWorkFolder, final CometWorkPacket packet) {
		final File parameterFile = new File(tempWorkFolder, "comet.parameters");

		// Replace the database path and write parameters out
		FileUtilities.writeStringToFile(parameterFile, packet.getSearchParams(), true);
		return parameterFile;
	}

	/**
	 * Given an output file name, creates a "name prefix" to be passed to Comet that would fool
	 * Comet into generating the required output file.
	 * <p/>
	 * This can fail if the requested output file suffix does not match .pep.xml or .sqt
	 *
	 * @param outputFile Name of the output file.
	 * @return String to pass to Comet as a {@code -N} parameter.
	 */
	private String getResultFileName(final File outputFile) {
		if (outputFile.getAbsolutePath().endsWith(PEP_XML)) {
			return trimExtension(outputFile, PEP_XML);
		} else if (outputFile.getAbsolutePath().endsWith(SQT)) {
			return trimExtension(outputFile, SQT);
		} else {
			throw new MprcException(String.format("Swift only supports Comet generating a %s output files. Requested file was %s", PEP_XML + ", " + SQT, outputFile.getName()));
		}
	}

	private String trimExtension(final File outputFile, final String extension) {
		return new File(outputFile.getParentFile(),
				outputFile.getName().substring(0, outputFile.getName().length() - extension.length())).getAbsolutePath();
	}

	@Override
	public String check() {
		LOGGER.info("Checking Comet worker");
		if (cometExecutable == null) {
			throw new MprcException("Comet excecutable not set!");
		}
		if (!cometExecutable.isFile()) {
			throw new MprcException(String.format("Comet executable not present [%s]", cometExecutable.getAbsolutePath()));
		}

		if (!cometExecutable.canExecute()) {
			throw new MprcException(String.format("Comet file does not have executable flag set [%s]", cometExecutable.getAbsolutePath()));
		}
		return null;
	}

	public void setMsconvertDaemon(DaemonConnection msconvertDaemon) {
		this.msconvertDaemon = msconvertDaemon;
	}

	public DaemonConnection getMsconvertDaemon() {
		return msconvertDaemon;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("cometWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {

		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"COMET", ".sqt", "Comet", true, "comet", new CometMappingFactory(),
				new String[]{TYPE},
				new String[]{CometCache.TYPE},
				new String[]{},
				21, false);


		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			CometWorker worker = null;
			try {
				worker = new CometWorker(FileUtilities.getAbsoluteFileForExecutables(config.getCometExecutable()));
				if(config.getMsconvert()!=null) {
					worker.setMsconvertDaemon((DaemonConnection) dependencies.createSingleton(config.getMsconvert()));
				} else {
					worker.setMsconvertDaemon(null);
				}
			} catch (final Exception e) {
				throw new MprcException("Comet worker could not be created.", e);
			}
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
		private String cometExecutable;
		private ServiceConfig msconvert;

		public Config() {
		}

		@Override
		public void save(ConfigWriter writer) {
			writer.put(COMET_EXECUTABLE, cometExecutable, "Path to Comet executable");
			writer.put(MSCONVERT, msconvert);
		}

		@Override
		public void load(ConfigReader reader) {
			cometExecutable = reader.get(COMET_EXECUTABLE);
			msconvert = (ServiceConfig) reader.getObject(MSCONVERT);
		}

		@Override
		public int getPriority() {
			return 0;
		}

		public File getCometExecutable() {
			return new File(cometExecutable);
		}

		public ServiceConfig getMsconvert() {
			return msconvert;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(COMET_EXECUTABLE, "Executable Path", "Comet executable path. Comet executables can be " +
					"<br/>found at <a href=\"http://sourceforge.net/projects/comet-ms/files/\"/>http://sourceforge.net/projects/comet-ms/files/</a>")
					.required()
					.executable(Arrays.asList(""))
					.defaultValue("comet")

					.property(MSCONVERT, MsconvertWorker.NAME, "Msconvert is needed to convert the Comet input file to MS2 format to accompany the .sqt (if requested)")
					.reference(MsconvertWorker.TYPE, MsconvertCache.TYPE, UiBuilder.NONE_TYPE);

		}
	}

	private static class MsconvertProgressListener implements ProgressListener {
		private final UserProgressReporter progressReporter;
		private File ms2File;
		private boolean finished;
		private Exception lastException;
		private final Object lock = new Object();

		private MsconvertProgressListener(UserProgressReporter progressReporter, final File ms2File) {
			this.progressReporter = progressReporter;
			setMs2File(ms2File);
		}

		@Override
		public void requestEnqueued(String hostString) {
			LOGGER.info("Msconvert task enqueued at " + hostString);
		}

		@Override
		public void requestProcessingStarted(String hostString) {
			LOGGER.info("Msconvert task processing started at " + hostString);
		}

		@Override
		public void requestProcessingFinished() {
			LOGGER.info("Msconvert task processing finished");
			synchronized (lock) {
				finished = true;
				lock.notifyAll();
			}
		}

		@Override
		public void requestTerminated(Exception e) {
			synchronized (lock) {
				finished = true;
				lastException = e;
				lock.notifyAll();
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			if (progressInfo instanceof NewLogFiles) {
				// We pass log information onto our parent
				progressReporter.reportProgress(progressInfo);
			} else if (progressInfo instanceof MsconvertResult) {
				// The msconvert tells us the output file is somewhere else
				setMs2File(((MsconvertResult) progressInfo).getOutputFile());
			}
		}

		public File getMs2File() {
			synchronized (lock) {
				return ms2File;
			}
		}

		public void setMs2File(final File ms2File) {
			synchronized (lock) {
				this.ms2File = ms2File;
			}
		}

		public Exception getLastException() {
			synchronized (lock) {
				return lastException;
			}
		}

		public void waitForFinished(final long timeout) throws InterruptedException {
			synchronized (lock) {
				while (!finished) {
					lock.wait(timeout);
				}
			}
		}

	}
}
