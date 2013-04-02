package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Myrimatch only supports forward databases.
 * <p/>
 * We assume that a database file will have all forward sequences at the beginning, followed by all reverse sequences.
 * The deployer simply finds out how many forward sequences are there, and then stores this number in a file, together
 * with the discovered reverse sequence prefix. Currently this algorithm does not work very well - need to improve!
 * <p/>
 * The setup is conveyed to Myrimatch. Myrimatch is then instructed to only consider the forward portion of the database.
 */
public final class MyrimatchDeploymentService extends DeploymentService<DeploymentResult> {

	private static final Logger LOGGER = Logger.getLogger(MyrimatchDeploymentService.class);
	public static final String TYPE = "myrimatchDeployer";
	public static final String NAME = "Myrimatch DB Deployer";
	public static final String DESC = "Myrimatch only uses forward databases.<br/>" +
			"The deployment determines if a database has reverse sequences.";

	public static final String DEFAULT_SEQUENCE_PREFIX = "Reversed_";
	public static final String NUM_FORWARD_ENTRIES = "numForwardEntries";
	public static final String DECOY_SEQUENCE_PREFIX = "decoySequencePrefix";

	private static final String DEPLOYABLE_DB_FOLDER = "deployableDbFolder";

	public MyrimatchDeploymentService() {

	}

	public synchronized DeploymentResult performDeployment(final DeploymentRequest request) {
		final MyrimatchDeploymentResult result = new MyrimatchDeploymentResult();

		if (!hasCachedDeployment(request, result)) {
			deploy(request, result);
		}

		return result;
	}

	@Override
	public DeploymentResult performUndeployment(final DeploymentRequest request) {
		final File infoFile = getDeploymentInfoFile(request);

		FileUtilities.quietDelete(infoFile);

		LOGGER.info("Myrimatch undeployment of database " + request.getShortName() + " completed successfully.");
		return new DeploymentResult();
	}

	private boolean hasCachedDeployment(final DeploymentRequest request, final MyrimatchDeploymentResult result) {
		final File curationFile = request.getCurationFile();

		if (!curationFile.exists()) {
			final MprcException mprcException = new MprcException("The file passed in the curation didn't exist: " + curationFile.getAbsolutePath());
			LOGGER.error(mprcException.getMessage());
			throw mprcException;
		}

		result.setDeployedFile(curationFile);

		final File infoFile = getDeploymentInfoFile(request);
		final Properties properties = loadInfoFile(infoFile);

		final long numForwardEntries = Long.parseLong(properties.getProperty(NUM_FORWARD_ENTRIES, "-1"));
		final String decoySequencePrefix = properties.getProperty(DECOY_SEQUENCE_PREFIX, DEFAULT_SEQUENCE_PREFIX);
		if (numForwardEntries >= 0) {
			result.setNumForwardEntries(numForwardEntries);
			result.setDecoySequencePrefix(decoySequencePrefix);
			return true;
		}
		return false;
	}

	private void deploy(final DeploymentRequest request, final MyrimatchDeploymentResult result) {
		final File curationFile = request.getCurationFile();
		LOGGER.info("Myrimatch deployment services started. Deployment file [" + curationFile.getAbsolutePath() + "]");

		final File infoFile = getDeploymentInfoFile(request);
		// Go through all database entries and see when they turn into reverse ones.
		// Reverse sequences must have matching accession number
		int numSequences = 0;

		String reversePrefix = request.getAnnotation().getDecoyRegex();
		final BufferedReader reader = FileUtilities.getReader(curationFile);
		final String reversePrefixStart = ">" + reversePrefix;
		try {
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith(">")) {
					if (line.startsWith(reversePrefixStart)) {
						break;
					}
					numSequences++;
				}
			}
		} catch (Exception e) {
			throw new MprcException("Myrimatch deployer could not determine reversed sequences in " + request.getShortName() + " database: " + curationFile.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}

		final Properties properties = new Properties();
		properties.setProperty(NUM_FORWARD_ENTRIES, String.valueOf(numSequences));
		properties.setProperty(DECOY_SEQUENCE_PREFIX, reversePrefix);

		saveInfoFile(infoFile, properties);

		result.setNumForwardEntries(numSequences);
		result.setDecoySequencePrefix(reversePrefix);

		LOGGER.info("Myrimatch deployment services completed. Deployment file [" + curationFile.getAbsolutePath() + "]");
	}

	private Properties loadInfoFile(final File infoFile) {
		final Properties properties = new Properties();
		if (infoFile.exists()) {
			try {
				properties.loadFromXML(FileUtilities.getInputStream(infoFile));
			} catch (Exception e) {
				throw new MprcException("Myrimatch deployer could not read previous deployment information", e);
			}
		}
		return properties;
	}

	private void saveInfoFile(final File infoFile, final Properties properties) {
		final FileOutputStream outputStream = FileUtilities.getOutputStream(infoFile);
		try {
			properties.storeToXML(outputStream, "Myrimatch database information");
		} catch (IOException e) {
			throw new MprcException("Cannot save Myrimatch database information to file " + infoFile.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(outputStream);
		}
	}

	private File getDeploymentInfoFile(final DeploymentRequest request) {
		return new File(getDeployableDbFolder(), request.getShortName() + ".info.xml");
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String deployableDbFolder;

		public Config() {
		}

		public Config(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		public String getDeployableDbFolder() {
			return deployableDbFolder;
		}

		public void setDeployableDbFolder(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(DEPLOYABLE_DB_FOLDER, getDeployableDbFolder());
		}

		@Override
		public void load(final ConfigReader reader) {
			setDeployableDbFolder(reader.get(DEPLOYABLE_DB_FOLDER));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("myrimatchDeploymentServiceFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final MyrimatchDeploymentService worker = new MyrimatchDeploymentService();
			worker.setDeployableDbFolder(new File(config.getDeployableDbFolder()).getAbsoluteFile());
			return worker;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(DEPLOYABLE_DB_FOLDER, "Database Folder", "Information about .fasta files will be put here.<br/>" +
							"Myrimatch needs to know the prefix for the decoy databases.")
					.required()
					.existingDirectory()
					.defaultValue("var/myrimatch_index");
		}
	}
}
