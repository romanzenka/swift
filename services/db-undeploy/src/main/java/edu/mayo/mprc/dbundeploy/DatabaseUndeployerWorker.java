package edu.mayo.mprc.dbundeploy;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.*;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Worker that processes database undeployment request. Given a request, it processes
 * by creating a DatabaseUndeployerRunner object and executing its run() method.
 */
public final class DatabaseUndeployerWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(DatabaseUndeployerWorker.class);

	public static final String TYPE = "databaseUndeployer";
	public static final String NAME = "FASTA Database Undeployer";
	public static final String DESC = "Removes previously deployed search databases from defined database deployers.";
	private FileTokenFactory fileTokenFactory;

	private DaemonConnection mascotDeployerDaemon;
	private DaemonConnection omssaDeployerDaemon;
	private DaemonConnection tandemDeployerDaemon;
	private DaemonConnection sequestDeployerDaemon;
	private DaemonConnection scaffoldDeployerDaemon;
	private DaemonConnection scaffold3DeployerDaemon;
	private CurationDao curationDao;

	private static final String MASCOT_DEPLOYER = "mascotDeployer";
	private static final String TANDEM_DEPLOYER = "tandemDeployer";
	private static final String SEQUEST_DEPLOYER = "sequestDeployer";
	private static final String OMSSA_DEPLOYER = "omssaDeployer";
	private static final String SCAFFOLD_DEPLOYER = "scaffoldDeployer";
	private static final String SCAFFOLD3_DEPLOYER = "scaffold3Deployer";

	public DatabaseUndeployerWorker(final FileTokenFactory fileTokenFactory, final CurationDao curationDao) {
		this.fileTokenFactory = fileTokenFactory;
		this.curationDao = curationDao;
	}

	public DaemonConnection getMascotDeployerDaemon() {
		return mascotDeployerDaemon;
	}

	public void setMascotDeployerDaemon(final DaemonConnection mascotDeployerDaemon) {
		this.mascotDeployerDaemon = mascotDeployerDaemon;
	}

	public DaemonConnection getOmssaDeployerDaemon() {
		return omssaDeployerDaemon;
	}

	public void setOmssaDeployerDaemon(final DaemonConnection omssaDeployerDaemon) {
		this.omssaDeployerDaemon = omssaDeployerDaemon;
	}

	public DaemonConnection getTandemDeployerDaemon() {
		return tandemDeployerDaemon;
	}

	public void setTandemDeployerDaemon(final DaemonConnection tandemDeployerDaemon) {
		this.tandemDeployerDaemon = tandemDeployerDaemon;
	}

	public DaemonConnection getSequestDeployerDaemon() {
		return sequestDeployerDaemon;
	}

	public void setSequestDeployerDaemon(final DaemonConnection sequestDeployerDaemon) {
		this.sequestDeployerDaemon = sequestDeployerDaemon;
	}

	public DaemonConnection getScaffoldDeployerDaemon() {
		return scaffoldDeployerDaemon;
	}

	public void setScaffoldDeployerDaemon(final DaemonConnection scaffoldDeployerDaemon) {
		this.scaffoldDeployerDaemon = scaffoldDeployerDaemon;
	}

	public DaemonConnection getScaffold3DeployerDaemon() {
		return scaffold3DeployerDaemon;
	}

	public void setScaffold3DeployerDaemon(final DaemonConnection scaffold3DeployerDaemon) {
		this.scaffold3DeployerDaemon = scaffold3DeployerDaemon;
	}

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		final DatabaseUndeployerWorkPacket undeployerWorkPacket = (DatabaseUndeployerWorkPacket) workPacket;

		final DatabaseUndeployerRunner undeployerRunner = new DatabaseUndeployerRunner(undeployerWorkPacket
				, getMascotDeployerDaemon(), getOmssaDeployerDaemon()
				, getTandemDeployerDaemon(), getSequestDeployerDaemon(), getScaffoldDeployerDaemon()
				, fileTokenFactory);

		undeployerRunner.run();

		final DatabaseUndeployerResult undeployerResult = undeployerRunner.getDatabaseUndeployerResult();

		boolean success = true;

		for (final UndeploymentTaskResult undeploymentResult : undeployerResult.getDatabaseUndeployerResults().values()) {
			success = success && undeploymentResult.wasSuccessful();
		}

		if (success) {
			curationDao.begin();
			try {
				final Curation curation = curationDao.findCuration(undeployerWorkPacket.getDbToUndeploy().getShortName());
				curationDao.deleteCuration(curation, new Change("Undeploying curation " + curation.getShortName(), new DateTime()));
				curationDao.commit();
			} catch (Exception e) {
				curationDao.rollback();
				// TODO: This needs to be reported to the user
				LOGGER.error("Could not delete curation from the database", e);
			}
		}

		progressReporter.reportProgress(undeployerResult);
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("databaseUndeployerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private FileTokenFactory fileTokenFactory;
		private CurationDao curationDao;

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final DatabaseUndeployerWorker worker = new DatabaseUndeployerWorker(fileTokenFactory, curationDao);

			if (config.mascotDeployer != null) {
				worker.setMascotDeployerDaemon((DaemonConnection) dependencies.createSingleton(config.mascotDeployer));
			}
			if (config.tandemDeployer != null) {
				worker.setTandemDeployerDaemon((DaemonConnection) dependencies.createSingleton(config.tandemDeployer));
			}
			if (config.sequestDeployer != null) {
				worker.setSequestDeployerDaemon((DaemonConnection) dependencies.createSingleton(config.sequestDeployer));
			}
			if (config.omssaDeployer != null) {
				worker.setOmssaDeployerDaemon((DaemonConnection) dependencies.createSingleton(config.omssaDeployer));
			}
			if (config.scaffoldDeployer != null) {
				worker.setScaffoldDeployerDaemon((DaemonConnection) dependencies.createSingleton(config.scaffoldDeployer));
			}
			if (config.scaffold3Deployer != null) {
				worker.setScaffold3DeployerDaemon((DaemonConnection) dependencies.createSingleton(config.scaffold3Deployer));
			}

			return worker;
		}

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		@Resource(name = "fileTokenFactory")
		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		public CurationDao getCurationDao() {
			return curationDao;
		}

		@Resource(name = "curationDao")
		public void setCurationDao(final CurationDao curationDao) {
			this.curationDao = curationDao;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private ServiceConfig scaffoldDeployer;
		private ServiceConfig scaffold3Deployer;
		private ServiceConfig omssaDeployer;
		private ServiceConfig sequestDeployer;
		private ServiceConfig tandemDeployer;
		private ServiceConfig mascotDeployer;

		public Config() {
		}

		public Config(final ServiceConfig scaffoldDeployer, final ServiceConfig scaffold3Deployer, final ServiceConfig omssaDeployer, final ServiceConfig sequestDeployer, final ServiceConfig tandemDeployer, final ServiceConfig mascotDeployer) {
			this.scaffoldDeployer = scaffoldDeployer;
			this.scaffold3Deployer = scaffold3Deployer;
			this.omssaDeployer = omssaDeployer;
			this.sequestDeployer = sequestDeployer;
			this.tandemDeployer = tandemDeployer;
			this.mascotDeployer = mascotDeployer;
		}

		public ServiceConfig getScaffoldDeployer() {
			return scaffoldDeployer;
		}

		public void setScaffoldDeployer(final ServiceConfig scaffoldDeployer) {
			this.scaffoldDeployer = scaffoldDeployer;
		}

		public ServiceConfig getScaffold3Deployer() {
			return scaffold3Deployer;
		}

		public void setScaffold3Deployer(final ServiceConfig scaffold3Deployer) {
			this.scaffold3Deployer = scaffold3Deployer;
		}

		public ServiceConfig getOmssaDeployer() {
			return omssaDeployer;
		}

		public void setOmssaDeployer(final ServiceConfig omssaDeployer) {
			this.omssaDeployer = omssaDeployer;
		}

		public ServiceConfig getSequestDeployer() {
			return sequestDeployer;
		}

		public void setSequestDeployer(final ServiceConfig sequestDeployer) {
			this.sequestDeployer = sequestDeployer;
		}

		public ServiceConfig getTandemDeployer() {
			return tandemDeployer;
		}

		public void setTandemDeployer(final ServiceConfig tandemDeployer) {
			this.tandemDeployer = tandemDeployer;
		}

		public ServiceConfig getMascotDeployer() {
			return mascotDeployer;
		}

		public void setMascotDeployer(final ServiceConfig mascotDeployer) {
			this.mascotDeployer = mascotDeployer;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(MASCOT_DEPLOYER, mascotDeployer);
			writer.put(TANDEM_DEPLOYER, tandemDeployer);
			writer.put(SEQUEST_DEPLOYER, sequestDeployer);
			writer.put(OMSSA_DEPLOYER, omssaDeployer);
			writer.put(SCAFFOLD_DEPLOYER, scaffoldDeployer);
			writer.put(SCAFFOLD3_DEPLOYER, scaffold3Deployer);
		}

		@Override
		public void load(final ConfigReader reader) {
			mascotDeployer = (ServiceConfig) reader.getObject(MASCOT_DEPLOYER);
			tandemDeployer = (ServiceConfig) reader.getObject(TANDEM_DEPLOYER);
			sequestDeployer = (ServiceConfig) reader.getObject(SEQUEST_DEPLOYER);
			omssaDeployer = (ServiceConfig) reader.getObject(OMSSA_DEPLOYER);
			scaffoldDeployer = (ServiceConfig) reader.getObject(SCAFFOLD_DEPLOYER);
			scaffold3Deployer = (ServiceConfig) reader.getObject(SCAFFOLD3_DEPLOYER);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(SEQUEST_DEPLOYER, "Sequest Database Deployer", "Database deployer must provide database undeployment functionality.")
					.reference("sequestDeployer", UiBuilder.NONE_TYPE)

					.property(TANDEM_DEPLOYER, "X!Tandem Database Deployer", "Database deployer must provide database undeployment functionality.")
					.reference("tandemDeployer", UiBuilder.NONE_TYPE)

					.property(OMSSA_DEPLOYER, "Omssa Database Deployer", "Database deployer must provide database undeployment functionality.")
					.reference("omssaDeployer", UiBuilder.NONE_TYPE)

					.property(SCAFFOLD_DEPLOYER, "Scaffold Database Deployer", "Database deployer must provide database undeployment functionality.")
					.reference("scaffoldDeployer", UiBuilder.NONE_TYPE)

					.property(SCAFFOLD3_DEPLOYER, "Scaffold3 Database Deployer", "Database deployer must provide database undeployment functionality.")
					.reference("scaffold3Deployer", UiBuilder.NONE_TYPE)

					.property(MASCOT_DEPLOYER, "Mascot Database Deployer", "Database deployer must provide database undeployment functionality.")
					.reference("mascotDeployer", "mockMascotDeployer", UiBuilder.NONE_TYPE);
		}
	}
}
