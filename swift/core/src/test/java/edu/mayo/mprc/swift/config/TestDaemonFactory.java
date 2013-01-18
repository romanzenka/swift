package edu.mayo.mprc.swift.config;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.config.ApplicationConfig;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.config.ServiceConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.dbundeploy.DatabaseUndeployerWorker;
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
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.testing.TestApplicationContext;
import edu.mayo.mprc.xtandem.XTandemDeploymentService;
import edu.mayo.mprc.xtandem.XTandemWorker;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

public final class TestDaemonFactory {
	private static MultiFactory table;
	private static File tempRootDir;

	private ApplicationConfig config;

	private static final String SWIFT_INSTALL_ROOT_PATH = "..";
	private static final String DATABASE_DEPLOYMENT_DIR = SWIFT_INSTALL_ROOT_PATH + "/install/swift/var/fasta/";

	@BeforeClass()
	public void setUp() throws IOException {
		tempRootDir = FileUtilities.createTempFolder();

		config = createSwiftConfig();

		table = TestApplicationContext.getResourceTable();
	}

	@Test
	public void shouldCreateDaemon() {
		final ApplicationConfig config = createSwiftConfig();

		final Daemon.Factory daemonFactory = TestApplicationContext.getDaemonFactory();
		final Daemon daemon = daemonFactory.createDaemon(config.getDaemonConfig("main"));
		daemon.start();
	}

	@Test
	public void shouldSaveAndLoad() throws IOException, SAXException {
		final ApplicationConfig config = createSwiftConfig();
		final File tempFile = File.createTempFile("swiftConfig", "xml");
		tempFile.deleteOnExit();

		config.save(tempFile, table);

		final String savedFileContents = Files.toString(tempFile, Charsets.UTF_8);

		Reader reader = null;
		try {
			reader = ResourceUtilities.getReader("classpath:edu/mayo/mprc/swift/config/fullConfig.xml", TestDaemonFactory.class);
			XMLUnit.setIgnoreWhitespace(true);
			final Diff diff = XMLUnit.compareXML(reader, savedFileContents);
			XMLAssert.assertXMLEqual("Config file not equal", diff, true);
		} finally {
			FileUtilities.closeQuietly(reader);
		}

		FileUtilities.quietDelete(tempFile);
	}

	private static ApplicationConfig createSwiftConfig() {
		final String BROKER_URI = "jms.vm://local?simplequeue=";

		final ApplicationConfig config = new ApplicationConfig();

		final DaemonConfig main = new DaemonConfig();
		main.setName("main");

		final SimpleRunner.Config runner = new SimpleRunner.Config();
		runner.setNumThreads(1);
		runner.setWorkerConfiguration(new MascotWorker.Config("http://localhost/mascot/"));  //Set up just to work on Carl (Windows server)
		final ServiceConfig mascot = new ServiceConfig("mascot", runner, BROKER_URI + "mascot");
		main.addService(mascot);

		final SimpleRunner.Config runner8 = new SimpleRunner.Config();
		runner8.setNumThreads(1);
		runner8.setWorkerConfiguration(new MascotDeploymentService.Config("engineRootFolder", "mascotDbMaintenanceUrl", DATABASE_DEPLOYMENT_DIR));
		final ServiceConfig mascotDeployer = new ServiceConfig("mascotDeployer", runner8, BROKER_URI + "mascotDeployer");
		main.addService(mascotDeployer);

		final SimpleRunner.Config runner2 = new SimpleRunner.Config();
		runner2.setNumThreads(2);
		runner2.setWorkerConfiguration(new OmssaWorker.Config("omssacl"));
		final ServiceConfig omssa = new ServiceConfig("omssa", runner2, BROKER_URI + "omssa");
		main.addService(omssa);

		final SimpleRunner.Config runner9 = new SimpleRunner.Config();
		runner9.setNumThreads(2);
		runner9.setWorkerConfiguration(new OmssaDeploymentService.Config("formatDbExe", DATABASE_DEPLOYMENT_DIR + "deployableDbFolder"));
		final ServiceConfig omssaDeployer = new ServiceConfig("omssaDeployer", runner9, BROKER_URI + "omssaDeployer");
		main.addService(omssaDeployer);

		final SimpleRunner.Config runner22 = new SimpleRunner.Config();
		runner22.setNumThreads(2);
		runner22.setWorkerConfiguration(new SequestWorker.Config("sequestCommand", "pvmHosts"));
		final ServiceConfig sequest = new ServiceConfig("sequest", runner22, BROKER_URI + "sequest");
		main.addService(sequest);

		final SimpleRunner.Config runner33 = new SimpleRunner.Config();
		runner33.setNumThreads(2);
		runner33.setWorkerConfiguration(new SequestDeploymentService.Config("deployableDbFolder", "engineRootFolder", "wineWrapperScript"));
		final ServiceConfig sequestDeployer = new ServiceConfig("sequestDeployer", runner33, BROKER_URI + "sequestDeployer");
		main.addService(sequestDeployer);

		final SimpleRunner.Config runner338 = new SimpleRunner.Config();
		runner338.setNumThreads(2);
		runner338.setWorkerConfiguration(new XTandemWorker.Config("tandemExecutable"));
		final ServiceConfig tandem = new ServiceConfig("tandem", runner338, BROKER_URI + "tandem");
		main.addService(tandem);

		final SimpleRunner.Config runner331 = new SimpleRunner.Config();
		runner331.setNumThreads(2);
		runner331.setWorkerConfiguration(new XTandemDeploymentService.Config());
		final ServiceConfig tandemDeployer = new ServiceConfig("tandemDeployer", runner331, BROKER_URI + "tandemDeployer");
		main.addService(tandemDeployer);

		final SimpleRunner.Config runner3 = new SimpleRunner.Config();
		runner3.setNumThreads(2);
		runner3.setWorkerConfiguration(new ScaffoldWorker.Config("dir", "javavm", "memory"));
		final ServiceConfig scaffold = new ServiceConfig("scaffold", runner3, BROKER_URI + "scaffold");
		main.addService(scaffold);

		final SimpleRunner.Config runner34 = new SimpleRunner.Config();
		runner34.setNumThreads(2);
		runner34.setWorkerConfiguration(new ScaffoldReportWorker.Config());
		final ServiceConfig scaffoldReport = new ServiceConfig("scaffoldReport", runner34, BROKER_URI + "scaffoldReport");
		main.addService(scaffoldReport);

		final SimpleRunner.Config runner35 = new SimpleRunner.Config();
		runner35.setNumThreads(3);
		runner35.setWorkerConfiguration(new QaWorker.Config("xvfbWrapperScript", "rScript"));
		final ServiceConfig qa = new ServiceConfig("qa", runner35, BROKER_URI + "qa");
		main.addService(qa);

		final SimpleRunner.Config runner11 = new SimpleRunner.Config();
		runner11.setNumThreads(1);
		runner11.setWorkerConfiguration(new ScaffoldDeploymentService.Config("scaffoldJavaVmPath", "deployableDbFolder", "engineRootFolder"));
		final ServiceConfig scaffoldDeployer = new ServiceConfig("scaffoldDeployer", runner11, BROKER_URI + "scaffoldDeployer");
		main.addService(scaffoldDeployer);

		final SimpleRunner.Config runner4 = new SimpleRunner.Config();
		runner4.setNumThreads(2);
		runner4.setWorkerConfiguration(new MSMSEvalWorker.Config("msmsEval", "test,test.txt"));
		final ServiceConfig msmsEval = new ServiceConfig("msmsEval", runner4, BROKER_URI + "msmsEval");
		main.addService(msmsEval);

		final SimpleRunner.Config runner5 = new SimpleRunner.Config();
		runner5.setNumThreads(2);
		final RawToMgfWorker.Config raw2mgfConfig = new RawToMgfWorker.Config("tempFolder", "wineconsole", "../install/swift/bin/util/unixXvfbWrapper.sh", SWIFT_INSTALL_ROOT_PATH + "/install/swift/bin/extract_msn/extract_msn.exe");
		runner5.setWorkerConfiguration(raw2mgfConfig);
		final ServiceConfig raw2mgf = new ServiceConfig("raw2mgf", runner5, BROKER_URI + "raw2mgf");
		main.addService(raw2mgf);

		final SimpleRunner.Config runner6 = new SimpleRunner.Config();
		runner6.setNumThreads(3);
		final MsconvertWorker.Config msconvertConfig = new MsconvertWorker.Config("run_msconvert.sh", "run_msaccess.sh");
		runner6.setWorkerConfiguration(msconvertConfig);
		final ServiceConfig msconvert = new ServiceConfig("msconvert", runner6, BROKER_URI + "msconvert");
		main.addService(msconvert);

		final SimpleRunner.Config runner72 = new SimpleRunner.Config();
		runner72.setNumThreads(2);
		runner72.setWorkerConfiguration(new MockMascotDeploymentService.Config());
		final ServiceConfig mockMascotDeployer = new ServiceConfig("mockMascotDeployer", runner72, BROKER_URI + "mockMascotDeployer");
		main.addService(mockMascotDeployer);

		final SimpleRunner.Config runner74 = new SimpleRunner.Config();
		runner74.setNumThreads(2);
		runner74.setWorkerConfiguration(new QstatDaemonWorker.Config());
		final ServiceConfig qstat = new ServiceConfig("qstat", runner74, BROKER_URI + "qstat");
		main.addService(qstat);

		final MgfToMgfWorker.Config mgfToMgfConfig = new MgfToMgfWorker.Config();
		final SimpleRunner.Config runner75 = new SimpleRunner.Config();
		runner75.setNumThreads(3);
		runner75.setWorkerConfiguration(mgfToMgfConfig);
		final ServiceConfig mgfToMgf = new ServiceConfig("mgfToMgf", runner75, BROKER_URI + "mgfToMgf");
		main.addService(mgfToMgf);

		final RAWDumpWorker.Config rawDumpWorker = new RAWDumpWorker.Config();
		final SimpleRunner.Config runner79 = new SimpleRunner.Config();
		runner79.setNumThreads(3);
		runner79.setWorkerConfiguration(rawDumpWorker);
		final ServiceConfig rawDump = new ServiceConfig("rawDump", runner79, BROKER_URI + "rawDump");
		main.addService(rawDump);

		final SimpleRunner.Config runner88 = new SimpleRunner.Config();
		runner88.setNumThreads(1);
		runner88.setWorkerConfiguration(new DatabaseUndeployerWorker.Config(scaffoldDeployer, null, omssaDeployer
				, sequestDeployer, tandemDeployer, mascotDeployer));
		final ServiceConfig databasUndeployer = new ServiceConfig("databaseUndeployer", runner88, BROKER_URI + "databaseUndeployer");
		main.addService(databasUndeployer);

		final SwiftSearcher.Config searcherConfig = new SwiftSearcher.Config(
				"fastaPath", "fastaArchivePath",
				"fastaUploadPath", raw2mgf, msconvert, mgfToMgf, rawDump, mascot, mascotDeployer, sequest,
				sequestDeployer, tandem, tandemDeployer, omssa, omssaDeployer, null, null, null, null, scaffold, scaffoldDeployer, null, null, scaffoldReport, qa, null, null, msmsEval, null);
		final SimpleRunner.Config runner76 = new SimpleRunner.Config();
		runner76.setNumThreads(1);
		runner76.setWorkerConfiguration(searcherConfig);

		final ServiceConfig searcher = new ServiceConfig("searcher", runner76, BROKER_URI + "searcher");
		main.addService(searcher);

		final WebUi.Config webUi = new WebUi.Config(searcher, "8080", "Swift 2.5", "C:\\", "file:///C:/", qstat, databasUndeployer, "C:\\");
		main.addResource(webUi);

		config.addDaemon(main);

		return config;
	}

	@AfterClass()
	public void cleanUp() {
		FileUtilities.cleanupTempFile(tempRootDir);
	}
}
