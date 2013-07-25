package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ServiceConfig;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.qa.RAWDumpWorker;
import edu.mayo.mprc.searchdb.RawFileMetaData;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.swift.search.task.RAWDumpTask;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workflow.engine.TaskBase;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.apache.log4j.Logger;
import org.joda.time.Interval;
import org.joda.time.ReadableInterval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads metadata about a given .RAW file into the database.
 * If a database entry already exists, it gets updated.
 *
 * @author Roman Zenka
 */
public final class LoadRawMetadata implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(LoadRawMetadata.class);
	public static final int BATCH_SIZE = 20;

	private DaemonConnection rawDump;
	private SearchDbDao searchDbDao;
	private FileTokenFactory fileTokenFactory;
	private int totalToLoad;
	private int loaded;

	@Override
	public String getName() {
		return "load-raw";
	}

	@Override
	public String getDescription() {
		return "Loads metadata about a given .RAW file into the database.";
	}

	/**
	 * Load given search results into the database.
	 * This is equivalent to a "shortened" Swift search that:
	 * 1) dumps .RAW metadata
	 * 2) dumps Scaffold spectrum report (if missing) using Scaffold 3
	 * 3) loads the FASTA database
	 * 4) loads the Scaffold dump
	 *
	 * @param environment The Swift environment to execute within.
	 */
	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		try {
			LOGGER.info("Loading RAW files into database");
			final long start = System.currentTimeMillis();

			final SwiftSearcher.Config config = environment.getSwiftSearcher();
			initializeConnections(environment, config);

			// Set up the database
			LoadToSearchDb.initializeDatabase(environment, config);

			totalToLoad = environment.getParameters().size();

			// Create a workflow for each file to load.
			// Run in batches not to flood the Sun Grid Engine
			final List<WorkflowEngine> workflowEngines = new ArrayList<WorkflowEngine>(BATCH_SIZE);
			for (final String rawFile : environment.getParameters()) {
				final File file = new File(rawFile);
				final WorkflowEngine workflowEngine = loadData(file, new File(environment.getDaemonConfig().getTempFolderPath()));
				workflowEngines.add(workflowEngine);

				if (workflowEngines.size() == BATCH_SIZE) {
					runTillDone(workflowEngines);
				}
			}

			runTillDone(workflowEngines);

			final long end = System.currentTimeMillis();

			final ReadableInterval interval = new Interval(start, end);
			LOGGER.info("Elapsed time: " + interval.toDuration().toString());

			return ExitCode.Ok;

		} catch (Exception e) {
			throw new MprcException("Could not load into Swift search database", e);
		}
	}

	private void runTillDone(final List<WorkflowEngine> engines) {
		while (!runEngines(engines)) {
		}
		engines.clear();
	}

	private boolean runEngines(final List<WorkflowEngine> engines) {
		boolean allDone = true;
		for (final WorkflowEngine engine : engines) {
			if (!engine.isDone()) {
				allDone = false;
				try {
					engine.run();
				} catch (MprcException e) {
					// SWALLOWED: We are okay with the engine failing, we log it and go on
					LOGGER.error("The load failed\n" + MprcException.getDetailedMessage(e));
				}
			}
		}
		return allDone;
	}

	/**
	 * Create a workflow engine that dumps .RAW file metadata, loads them into the database and counts the result.
	 *
	 * @param file       Raw file to load.
	 * @param tempFolder Temp folder
	 * @return Workflow engine that loads the file.
	 */
	private WorkflowEngine loadData(final File file, final File tempFolder) {
		final WorkflowEngine workflowEngine = new WorkflowEngine("load " + file.getAbsolutePath());

		final File temp = FileUtilities.createTempFolder(tempFolder, "dump", true);

		final RAWDumpTask rawDumpTask = new RAWDumpTask(workflowEngine, file, temp, rawDump, fileTokenFactory, false);
		workflowEngine.addTask(rawDumpTask);

		final LoadRawTask loadRawTask = new LoadRawTask(workflowEngine, rawDumpTask, searchDbDao);
		workflowEngine.addTask(loadRawTask);
		loadRawTask.addDependency(rawDumpTask);

		final SuccessfulLoadCounter counter = new SuccessfulLoadCounter(workflowEngine, file, temp);
		workflowEngine.addTask(counter);
		counter.addDependency(loadRawTask);

		return workflowEngine;
	}

	private void initializeConnections(final SwiftEnvironment environment, final SwiftSearcher.Config config) {
		rawDump = getConnection(environment, config.getRawdump(), RAWDumpWorker.NAME);
	}

	private DaemonConnection getConnection(final SwiftEnvironment environment, final ServiceConfig serviceConfig, final String workerName) {
		final DaemonConnection connection = environment.getConnection(serviceConfig);
		if (connection == null) {
			throw new MprcException("No " + workerName + " worker defined.");
		}
		return connection;
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	/**
	 * Counts all the successful DB loads.
	 */
	private final class SuccessfulLoadCounter extends TaskBase {
		private final File file;
		private final File temp;

		private SuccessfulLoadCounter(final WorkflowEngine workflowEngine, final File file, final File temp) {
			super(workflowEngine);
			this.file = file;
			this.temp = temp;
		}

		@Override
		public void run() {
			FileUtilities.deleteNow(temp);
			loaded++;
			LOGGER.info("Loaded " + file.getAbsolutePath() + " (" + loaded + " out of " + totalToLoad + ")");
			setState(TaskState.COMPLETED_SUCCESFULLY);
		}
	}

	/**
	 * Loads the RAW metadata into the database.
	 */
	private static final class LoadRawTask extends TaskBase {
		private final RAWDumpTask rawDumpTask;
		private final SearchDbDao dao;

		private LoadRawTask(final WorkflowEngine workflowEngine, final RAWDumpTask rawDumpTask, final SearchDbDao dao) {
			super(workflowEngine);
			this.rawDumpTask = rawDumpTask;
			this.dao = dao;
		}

		@Override
		public void run() {
			dao.begin();
			try {
				final RawFileMetaData metadata = rawDumpTask.getRawFileMetadata();
				dao.updateTandemMassSpectrometrySample(metadata.parse());
				dao.commit();
			} catch (Exception e) {
				dao.rollback();
				throw new MprcException("Could not load RAW file [" + rawDumpTask.getRawFile().getAbsolutePath() + "] into database", e);
			}
			setState(TaskState.COMPLETED_SUCCESFULLY);
		}
	}
}
