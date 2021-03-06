package edu.mayo.mprc.mascot;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Test-only mascot deployment. Creates extremely simplistic {@link edu.mayo.mprc.enginedeployment.DeploymentResult}
 * for given request.
 */
public final class MockMascotDeploymentService extends WorkerBase {
	public static final String TYPE = "mockMascotDeployer";
	public static final String NAME = "Mock Mascot DB Deployer";
	public static final String DESC = "If for some reason you cannot deploy new databases to mascot, use this 'mock deployer' that pretends the database was already deployed. You need to load the databases from Mascot to Swift before using this.";

	public MockMascotDeploymentService() {
	}

	@Override
	protected void process(WorkPacket workPacket, File tempWorkFolder, UserProgressReporter progressReporter) {
		progressReporter.reportProgress(new DeploymentResult());
	}

	@Override
	public File createTempWorkFolder() {
		return null;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("mockMascotDeploymentServiceFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new MockMascotDeploymentService();
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			// No UI needed
		}
	}
}
