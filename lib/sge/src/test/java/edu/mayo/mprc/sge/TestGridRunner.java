package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ServiceConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.DaemonRequest;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Zenka
 */
public final class TestGridRunner {
	private File baseFolder;
	private File logFolder; /* Log within the base folder */
	private File tempFolder; /* Temp folder within the base folder */
	private GridRunner runner;

	@BeforeTest
	public void setup() {
		baseFolder = FileUtilities.createTempFolder();
		logFolder = new File(baseFolder, "log");
		FileUtilities.ensureFolderExists(logFolder);
		tempFolder = new File(baseFolder, "temp");
		FileUtilities.ensureFolderExists(tempFolder);

		HelloWorldWorker.Config workerConfig = new HelloWorldWorker.Config();

		GridRunner.Config runnerConfig = new GridRunner.Config(workerConfig);

		GridRunner.Factory runnerFactory = new GridRunner.Factory();

		DaemonConfig daemonConfig = new DaemonConfig();
		daemonConfig.setName("test-daemon");
		daemonConfig.setSharedFileSpacePath(baseFolder.getAbsolutePath());
		daemonConfig.setTempFolderPath(tempFolder.getAbsolutePath());
		daemonConfig.setLogOutputFolder(logFolder.getAbsolutePath());

		DaemonConfigInfo daemonConfigInfo = daemonConfig.createDaemonConfigInfo();

		FileTokenFactory fileTokenFactory = new FileTokenFactory(daemonConfigInfo);

		runnerFactory.setFileTokenFactory(fileTokenFactory);
		GridScriptFactory gridScriptFactory = new GridScriptFactory();
		gridScriptFactory.setSwiftLibDirectory(new File(baseFolder, "lib").getAbsolutePath());
		runnerFactory.setGridScriptFactory(gridScriptFactory);
		GridEngineJobManager jobManager = mock(GridEngineJobManager.class);
		runnerFactory.setGridEngineManager(jobManager);
		ServiceFactory serviceFactory = mock(ServiceFactory.class);
		runnerFactory.setServiceFactory(serviceFactory);

		ServiceConfig serviceConfig = new ServiceConfig("hello-world", runnerConfig);
		serviceConfig.setParentConfig(daemonConfig);

		runnerConfig.setParentConfig(serviceConfig);
		DaemonConnection daemonConnection = mock(DaemonConnection.class);
		when(daemonConnection.getConnectionName()).thenReturn("hello-world");
		when(daemonConnection.getFileTokenFactory()).thenReturn(fileTokenFactory);

		runner = runnerFactory.create(runnerConfig, new DependencyResolver(null));
		runner.setDaemonConnection(daemonConnection);
		Daemon daemon = new Daemon();
		daemon.setSharedFileSpace(baseFolder);
		daemon.setTempFolder(tempFolder);
		runner.setDaemon(daemon);
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(baseFolder);
	}

	@Test
	public void shouldLogProperly() {
		DaemonRequest request = mock(DaemonRequest.class);
		when(request.getWorkPacket()).thenReturn(new HelloWorldWorkPacket(false));

		for (int i = 0; i < 2000; i++) {
			runner.processRequest(request);
		}

		// We must have produced 2000 work packets without crashing
		Assert.assertEquals(tempFolder.listFiles().length, 2000);
	}

}
