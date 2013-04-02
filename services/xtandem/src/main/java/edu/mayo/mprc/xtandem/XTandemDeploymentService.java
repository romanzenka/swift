package edu.mayo.mprc.xtandem;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a dummy class provided only to keep the logic same for all search engine types.
 * There is no database deployment needed for X!Tandem.
 */
public final class XTandemDeploymentService extends DeploymentService<DeploymentResult> {

	private static final Logger LOGGER = Logger.getLogger(XTandemDeploymentService.class);
	public static final String TYPE = "tandemDeployer";
	public static final String NAME = "X!Tandem DB Deployer";
	public static final String DESC = "X!Tandem does not require database deployment, but this module is needed anyway to make the search engines more uniform.";

	public XTandemDeploymentService() {

	}

	public synchronized DeploymentResult performDeployment(final DeploymentRequest request) {
		final DeploymentResult result = new DeploymentResult();

		final File curationFile = request.getCurationFile();

		LOGGER.info("XTandem deployment services started. Deployment file [" + curationFile.getAbsolutePath() + "]");

		if (!curationFile.exists()) {
			final MprcException mprcException = new MprcException("The file passed in the curation didn't exist: " + curationFile.getAbsolutePath());
			LOGGER.error(mprcException.getMessage());
			throw mprcException;
		}

		result.setDeployedFile(curationFile);

		LOGGER.info("XTandem deployment services completed. Deployment file [" + curationFile.getAbsolutePath() + "]");

		return result;
	}

	@Override
	public DeploymentResult performUndeployment(final DeploymentRequest request) {
		LOGGER.info("XTandem undeployment of database " + request.getShortName() + " completed successfully.");
		return new DeploymentResult();
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		public Config() {
		}

		public void save(final ConfigWriter writer) {
		}

		public void load(final ConfigReader reader) {
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("xTandemDeploymentServiceFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new XTandemDeploymentService();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
		}
	}
}
