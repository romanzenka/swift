package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FixTag;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.DatabaseUtilities;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Validates the database. Initializes the database when needed. This is a master class that is responsible
 * for all database-related initialization.
 */
public final class DatabaseValidator implements RuntimeInitializer {

	private List<DaoBase> daoList;
	private Database database;
	private SwiftSearcher.Config searcherConfig;
	private DaemonConfig daemonConfig;
	private List<RuntimeInitializer> runtimeInitializers;
	private DatabaseFileTokenFactory fileTokenFactory;
	private RunningApplicationContext runningApplicationContext;

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

	/**
	 * Initialize the connection to the database.
	 * <p/>
	 * Initialize the {@link FileTokenFactory}.
	 * <p/>
	 * Open a session and a transaction, making everything ready to write into the database.
	 *
	 * @param schemaInitialization How to initialize the database.
	 */
	private void beginTransaction(final DatabaseUtilities.SchemaInitialization schemaInitialization) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("action", schemaInitialization.getValue());
		database.install(params);

		database.begin();
	}

	private void commitTransaction() {
		database.commit();
	}

	private void rollbackTransaction() {
		database.rollback();
	}

	@Override
	public String check() {
		final Future<String> future = EXECUTOR.submit(new Callable<String>() {
			@Override
			public String call() {
				String errors = "";
				try {
					// Before checking, update the schema
					beginTransaction(DatabaseUtilities.SchemaInitialization.Update);

					String initializationToDo = null;

					// Go through a list of RuntimeInitializer, stop when one of them reports it is not ready
					for (final RuntimeInitializer initializer : runtimeInitializers) {
						final String result = initializer.check();
						database.getSession().flush();
						if (result != null) {
							initializationToDo = result;
							break;
						}
					}

					if (initializationToDo != null) {
						errors += "Database is not initialized: " + initializationToDo + " - " + FixTag.getTag(
								DatabaseUtilities.SchemaInitialization.Update.getValue(), "Initialize Database");
					}
					commitTransaction();
				} catch (Exception e) {
					errors += "Database connection could not be established.<br/>Error: " + e.getMessage()
							+ "<br/>Database may not exist. " + FixTag.getTag(
							DatabaseUtilities.SchemaInitialization.Create.getValue(), "Create Database");
					rollbackTransaction();
				}

				return "".equals(errors) ? null : errors;
			}
		});

		try {
			return future.get();
		} catch (Exception e) {
			throw new MprcException("Could not check the database", e);
		}
	}

	@Override
	/**
	 * @param params Recognizes "action" key that can be one of
	 * {@link DatabaseUtilities.SchemaInitialization#getValue()}.
	 */
	public void install(final Map<String, String> params) {
		final String action = params.get("action");
		final HashMap<String, String> newParams = new HashMap<String, String>(params);
		newParams.put(CurationInitializer.FASTA_FOLDER, getSearcherConfig().getFastaPath());
		newParams.put(CurationInitializer.FASTA_ARCHIVE_FOLDER, getSearcherConfig().getFastaArchivePath());

		final Future<?> future = EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				try {
					DatabaseUtilities.SchemaInitialization initialization = DatabaseUtilities.SchemaInitialization.getForValue(action);

					beginTransaction(initialization);

					for (final RuntimeInitializer initializer : runtimeInitializers) {
						initializer.install(newParams);
						database.getSession().flush();
						// We completely wipe out the caches between the initialization steps to prevent
						// huge memory consumption.
						database.getSession().clear();
					}

					commitTransaction();
				} catch (Exception e) {
					rollbackTransaction();
					throw new MprcException(e);
				}
			}
		});

		try {
			future.get();
		} catch (Exception e) {
			throw new MprcException("Failed to initialize the database", e);
		}
	}

	public void setSearcherConfig(SwiftSearcher.Config searcherConfig) {
		this.searcherConfig = searcherConfig;
	}

	public SwiftSearcher.Config getSearcherConfig() {
		if (searcherConfig == null && runningApplicationContext != null) {
			ServiceConfig config = runningApplicationContext.getDaemonConfig().firstServiceOfType(SwiftSearcher.Config.class);
			ResourceConfig workerConfig = config.getRunner().getWorkerConfiguration();
			if (!(workerConfig instanceof SwiftSearcher.Config)) {
				ExceptionUtilities.throwCastException(config, SwiftSearcher.Config.class);
				return null;
			}
			searcherConfig = (SwiftSearcher.Config) workerConfig;
		}
		return searcherConfig;
	}

	public void setDaemonConfig(DaemonConfig daemonConfig) {
		this.daemonConfig = daemonConfig;
	}

	public DaemonConfig getDaemonConfig() {
		if (daemonConfig == null && runningApplicationContext != null) {
			daemonConfig = runningApplicationContext.getDaemonConfig();
		}
		return daemonConfig;
	}

	public Database getDatabase() {
		return database;
	}

	@Resource(name = "database")
	public void setDatabase(final Database database) {
		this.database = database;
	}

	public List<DaoBase> getDaoList() {
		return daoList;
	}

	@Resource
	public void setDaoList(final List<DaoBase> daoList) {
		this.daoList = daoList;
	}

	public List<RuntimeInitializer> getRuntimeInitializers() {
		return runtimeInitializers;
	}

	public void setRuntimeInitializers(final List<RuntimeInitializer> runtimeInitializers) {
		this.runtimeInitializers = runtimeInitializers;
	}

	public RunningApplicationContext getRunningApplicationContext() {
		return runningApplicationContext;
	}

	public void setRunningApplicationContext(final RunningApplicationContext context) {
		runningApplicationContext = context;
	}

	public DatabaseFileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final DatabaseFileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}
}
