package edu.mayo.mprc.swift.config;

import com.google.common.collect.Lists;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.mascot.MascotCache;
import edu.mayo.mprc.mascot.MascotDeploymentService;
import edu.mayo.mprc.mascot.MascotWorker;
import edu.mayo.mprc.mascot.MockMascotDeploymentService;
import edu.mayo.mprc.mgf2mgf.MgfToMgfWorker;
import edu.mayo.mprc.msconvert.MsconvertWorker;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.omssa.OmssaDeploymentService;
import edu.mayo.mprc.omssa.OmssaWorker;
import edu.mayo.mprc.qa.QaWorker;
import edu.mayo.mprc.qa.RAWDumpWorker;
import edu.mayo.mprc.qstat.QstatDaemonWorker;
import edu.mayo.mprc.raw2mgf.RawToMgfWorker;
import edu.mayo.mprc.scaffold.ScaffoldDeploymentService;
import edu.mayo.mprc.scaffold.ScaffoldWorker;
import edu.mayo.mprc.scaffold.report.ScaffoldReportWorker;
import edu.mayo.mprc.sequest.SequestDeploymentService;
import edu.mayo.mprc.sequest.SequestWorker;
import edu.mayo.mprc.swift.WebUi;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import edu.mayo.mprc.utilities.testing.TestApplicationContext;
import edu.mayo.mprc.xtandem.XTandemDeploymentService;
import edu.mayo.mprc.xtandem.XTandemWorker;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.Collection;

public final class TestDaemonFactory {
	private static MultiFactory table;
	private static File tempRootDir;

	private ApplicationConfig config;

	private static final String SWIFT_INSTALL_ROOT_PATH = "..";
	private static final String DATABASE_DEPLOYMENT_DIR = SWIFT_INSTALL_ROOT_PATH + "/install/swift/var/fasta/";

	@BeforeClass
	public void setUp() {
		tempRootDir = FileUtilities.createTempFolder();

		config = createSwiftConfig();

		table = TestApplicationContext.getResourceTable();
	}

	@Test
	public void shouldCreateDaemon() {
		final ApplicationConfig config = createSwiftConfig();

		final Daemon daemon = (Daemon) table.create(config.getDaemonConfig("main"), new DependencyResolver(table));
		daemon.start();
	}

	@Test
	public void shouldSaveDir() {
		final ApplicationConfig config = createSwiftConfig();
		String result = configToString(config);

		Assert.assertEquals(
				TestingUtilities.compareStringToResourceByLine(
						result,
						"edu/mayo/mprc/swift/config/fullConfig.conf"), null);
	}

	private String configToString(ApplicationConfig config) {
		final StringWriter stringWriter = new StringWriter();
		AppConfigWriter writer = new AppConfigWriter(stringWriter, table);
		writer.save(config);
		return stringWriter.toString();
	}

	@Test
	public void shouldSaveDirMultiReferences() {
		final ApplicationConfig config = new ApplicationConfig(new DependencyResolver(table));
		final DaemonConfig daemon1 = new DaemonConfig();
		daemon1.setName("daemon1");
		final DaemonConfig daemon2 = new DaemonConfig();
		daemon2.setName("daemon2");
		config.addDaemon(daemon1);
		config.addDaemon(daemon2);
		final MessageBroker.Config messageBroker = new MessageBroker.Config();
		daemon1.addResource(messageBroker);
		final MascotCache.Config mascotCache = new MascotCache.Config();
		final MascotWorker.Config mascot = new MascotWorker.Config("http://mascot");
		final SimpleRunner.Config mascotRunner = new SimpleRunner.Config();
		mascotRunner.setWorkerConfiguration(mascot);
		final ServiceConfig mascotService = new ServiceConfig("mascot", mascotRunner);
		mascotCache.setService(mascotService);
		final SimpleRunner.Config mascotCacheRunner = new SimpleRunner.Config();
		mascotCacheRunner.setWorkerConfiguration(mascotCache);
		final ServiceConfig mascotCacheService = new ServiceConfig("mascotCache", mascotCacheRunner);
		daemon1.addResource(mascotCacheService);
		daemon1.addResource(mascotService);
		daemon2.addResource(mascotService);

		final String result = configToString(config);

		Assert.assertEquals(
				TestingUtilities.compareStringToResourceByLine(
						result,
						"edu/mayo/mprc/swift/config/twoDaemons.conf"), null);
	}


	private static ApplicationConfig createSwiftConfig() {
		final ApplicationConfig config = new ApplicationConfig(new DependencyResolver(table));

		final DaemonConfig main = new DaemonConfig();
		main.setName("main");

		final SimpleRunner.Config runner = new SimpleRunner.Config();
		runner.setNumThreads(1);
		runner.setWorkerConfiguration(new MascotWorker.Config("http://localhost/mascot/"));  //Set up just to work on Carl (Windows server)
		final ServiceConfig mascot = new ServiceConfig("mascot", runner);
		main.addResource(mascot);

		final SimpleRunner.Config runner8 = new SimpleRunner.Config();
		runner8.setNumThreads(1);
		runner8.setWorkerConfiguration(new MascotDeploymentService.Config("engineRootFolder", "mascotDbMaintenanceUrl", DATABASE_DEPLOYMENT_DIR));
		final ServiceConfig mascotDeployer = new ServiceConfig("mascotDeployer", runner8);
		main.addResource(mascotDeployer);

		final SimpleRunner.Config runner2 = new SimpleRunner.Config();
		runner2.setNumThreads(2);
		runner2.setWorkerConfiguration(new OmssaWorker.Config("omssacl"));
		final ServiceConfig omssa = new ServiceConfig("omssa", runner2);
		main.addResource(omssa);

		final SimpleRunner.Config runner9 = new SimpleRunner.Config();
		runner9.setNumThreads(2);
		runner9.setWorkerConfiguration(new OmssaDeploymentService.Config("formatDbExe", DATABASE_DEPLOYMENT_DIR + "deployableDbFolder"));
		final ServiceConfig omssaDeployer = new ServiceConfig("omssaDeployer", runner9);
		main.addResource(omssaDeployer);

		final SimpleRunner.Config runner22 = new SimpleRunner.Config();
		runner22.setNumThreads(2);
		runner22.setWorkerConfiguration(new SequestWorker.Config("sequestCommand", "pvmHosts"));
		final ServiceConfig sequest = new ServiceConfig("sequest", runner22);
		main.addResource(sequest);

		final SimpleRunner.Config runner33 = new SimpleRunner.Config();
		runner33.setNumThreads(2);
		runner33.setWorkerConfiguration(new SequestDeploymentService.Config("deployableDbFolder", "engineRootFolder", "wineWrapperScript"));
		final ServiceConfig sequestDeployer = new ServiceConfig("sequestDeployer", runner33);
		main.addResource(sequestDeployer);

		final SimpleRunner.Config runner338 = new SimpleRunner.Config();
		runner338.setNumThreads(2);
		runner338.setWorkerConfiguration(new XTandemWorker.Config("tandemExecutable"));
		final ServiceConfig tandem = new ServiceConfig("tandem", runner338);
		main.addResource(tandem);

		final SimpleRunner.Config runner331 = new SimpleRunner.Config();
		runner331.setNumThreads(2);
		runner331.setWorkerConfiguration(new XTandemDeploymentService.Config());
		final ServiceConfig tandemDeployer = new ServiceConfig("tandemDeployer", runner331);
		main.addResource(tandemDeployer);

		final SimpleRunner.Config runner3 = new SimpleRunner.Config();
		runner3.setNumThreads(2);
		runner3.setWorkerConfiguration(new ScaffoldWorker.Config());
		final ServiceConfig scaffold = new ServiceConfig("scaffold", runner3);
		main.addResource(scaffold);

		final SimpleRunner.Config runner34 = new SimpleRunner.Config();
		runner34.setNumThreads(2);
		runner34.setWorkerConfiguration(new ScaffoldReportWorker.Config());
		final ServiceConfig scaffoldReport = new ServiceConfig("scaffoldReport", runner34);
		main.addResource(scaffoldReport);

		final SimpleRunner.Config runner35 = new SimpleRunner.Config();
		runner35.setNumThreads(3);
		runner35.setWorkerConfiguration(new QaWorker.Config("xvfbWrapperScript", "rScript"));
		final ServiceConfig qa = new ServiceConfig("qa", runner35);
		main.addResource(qa);

		final SimpleRunner.Config runner11 = new SimpleRunner.Config();
		runner11.setNumThreads(1);
		runner11.setWorkerConfiguration(new ScaffoldDeploymentService.Config("deployableDbFolder"));
		final ServiceConfig scaffoldDeployer = new ServiceConfig("scaffoldDeployer", runner11);
		main.addResource(scaffoldDeployer);

		final SimpleRunner.Config runner4 = new SimpleRunner.Config();
		runner4.setNumThreads(2);
		runner4.setWorkerConfiguration(new MSMSEvalWorker.Config("msmsEval", "test,test.txt"));
		final ServiceConfig msmsEval = new ServiceConfig("msmsEval", runner4);
		main.addResource(msmsEval);

		final SimpleRunner.Config runner5 = new SimpleRunner.Config();
		runner5.setNumThreads(2);
		final RawToMgfWorker.Config raw2mgfConfig = new RawToMgfWorker.Config("tempFolder", "wineconsole", "../install/swift/bin/util/unixXvfbWrapper.sh", SWIFT_INSTALL_ROOT_PATH + "/install/swift/bin/extract_msn/extract_msn.exe");
		runner5.setWorkerConfiguration(raw2mgfConfig);
		final ServiceConfig raw2mgf = new ServiceConfig("raw2mgf", runner5);
		main.addResource(raw2mgf);

		final SimpleRunner.Config runner6 = new SimpleRunner.Config();
		runner6.setNumThreads(3);
		final MsconvertWorker.Config msconvertConfig = new MsconvertWorker.Config("run_msconvert.sh", "run_msaccess.sh");
		runner6.setWorkerConfiguration(msconvertConfig);
		final ServiceConfig msconvert = new ServiceConfig("msconvert", runner6);
		main.addResource(msconvert);

		final SimpleRunner.Config runner72 = new SimpleRunner.Config();
		runner72.setNumThreads(2);
		runner72.setWorkerConfiguration(new MockMascotDeploymentService.Config());
		final ServiceConfig mockMascotDeployer = new ServiceConfig("mockMascotDeployer", runner72);
		main.addResource(mockMascotDeployer);

		final SimpleRunner.Config runner74 = new SimpleRunner.Config();
		runner74.setNumThreads(2);
		runner74.setWorkerConfiguration(new QstatDaemonWorker.Config());
		final ServiceConfig qstat = new ServiceConfig("qstat", runner74);
		main.addResource(qstat);

		final MgfToMgfWorker.Config mgfToMgfConfig = new MgfToMgfWorker.Config();
		final SimpleRunner.Config runner75 = new SimpleRunner.Config();
		runner75.setNumThreads(3);
		runner75.setWorkerConfiguration(mgfToMgfConfig);
		final ServiceConfig mgfToMgf = new ServiceConfig("mgfToMgf", runner75);
		main.addResource(mgfToMgf);

		final RAWDumpWorker.Config rawDumpWorker = new RAWDumpWorker.Config();
		final SimpleRunner.Config runner79 = new SimpleRunner.Config();
		runner79.setNumThreads(3);
		runner79.setWorkerConfiguration(rawDumpWorker);
		final ServiceConfig rawDump = new ServiceConfig("rawDump", runner79);
		main.addResource(rawDump);

		Collection<SearchEngine.Config> engineConfigs = Lists.newArrayList(
				new SearchEngine.Config("MASCOT", "2.4", mascot, mascotDeployer),
				new SearchEngine.Config("SEQUEST", "v27", sequest, sequestDeployer),
				new SearchEngine.Config("TANDEM", "2013.2.01", tandem, tandemDeployer),
				new SearchEngine.Config("OMSSA", "0.1", omssa, omssaDeployer),
				new SearchEngine.Config("SCAFFOLD", "2.6.0", scaffold, scaffoldDeployer)
		);
		final SwiftSearcher.Config searcherConfig = new SwiftSearcher.Config(
				"fastaPath", "fastaArchivePath",
				"fastaUploadPath", raw2mgf, msconvert, mgfToMgf, rawDump,
				engineConfigs, scaffoldReport, qa, null, null, msmsEval, null);
		final SimpleRunner.Config runner76 = new SimpleRunner.Config();
		runner76.setNumThreads(1);
		runner76.setWorkerConfiguration(searcherConfig);

		final ServiceConfig searcher = new ServiceConfig("searcher", runner76);
		main.addResource(searcher);

		final WebUi.Config webUi = new WebUi.Config(searcher, "8080", "Swift 2.5", "C:\\", "file:///C:/", qstat, null);
		main.addResource(webUi);

		final MessageBroker.Config brokerConfig = MessageBroker.Config.getEmbeddedBroker();
		main.addResource(brokerConfig);

		config.addDaemon(main);

		return config;
	}

	@AfterClass
	public void cleanUp() {
		FileUtilities.cleanupTempFile(tempRootDir);
	}
}
