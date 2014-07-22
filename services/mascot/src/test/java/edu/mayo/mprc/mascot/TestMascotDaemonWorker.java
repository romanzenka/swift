package edu.mayo.mprc.mascot;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.TestMappingContextBase;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.log.SimpleParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

public final class TestMascotDaemonWorker {
	private static final Logger LOGGER = Logger.getLogger(TestMascotDaemonWorker.class);
	private File mascotTemp;
	private File inputMgfFile;
	private File inputMgfFolder;
	private static final String MASCOT_URL = "http://mascot.mayo.edu";
	private static final String TEST_MASCOT_DB = "Current_SP";

	@BeforeClass
	public void setup() throws IOException {
		mascotTemp = FileUtilities.createTempFolder();
		inputMgfFolder = Installer.mgfFiles(null, Installer.Action.INSTALL);
		inputMgfFile = new File(inputMgfFolder, "test.mgf");
	}

	@AfterClass
	public void teardown() {
		Installer.mgfFiles(inputMgfFolder, Installer.Action.UNINSTALL);
		FileUtilities.cleanupTempFile(mascotTemp);
	}

	@Test
	public void shouldProvideCorrectCgiUrl() throws MalformedURLException {
		Assert.assertEquals(MascotWorker.mascotCgiUrl(
						new URL("http://mascot.mayo.edu/")),
				new URL("http://mascot.mayo.edu/" + MascotWorker.MASCOT_CGI),
				"Mascot CGI script path generated incorrectly");
		Assert.assertEquals(MascotWorker.mascotCgiUrl(
						new URL("http://crick4.mayo.edu:2080/mascot/")),
				new URL("http://crick4.mayo.edu:2080/mascot/" + MascotWorker.MASCOT_CGI),
				"Mascot CGI script path generated incorrectly");

		// CAREFUL! You always MUST provide the trailing slash
		Assert.assertEquals(MascotWorker.mascotCgiUrl(
						new URL("http://crick4.mayo.edu:2080/mascot")),
				new URL("http://crick4.mayo.edu:2080/" + MascotWorker.MASCOT_CGI),
				"Mascot CGI script path generated incorrectly");

	}

	@Test
	public void runMascotWorker() throws IOException {
		final File mascotOut = new File(mascotTemp, "mascot.dat");

		final String mascotParams = createMascotParams();
		final MascotWorker.Config config = new MascotWorker.Config();
		config.put(MascotWorker.MASCOT_URL, MASCOT_URL);
		final MascotWorker.Factory factory = new MascotWorker.Factory();

		final Worker worker = factory.create(config, null);

		final MascotWorkPacket workPacket = new MascotWorkPacket(mascotOut, mascotParams, inputMgfFile, TEST_MASCOT_DB, "0", false, false);
		WorkPacketBase.simulateTransfer(workPacket);

		worker.processRequest(workPacket, new ProgressReporter() {
			@Override
			public void reportStart(final String hostString) {
				LOGGER.info("Started processing on " + hostString);
			}

			@Override
			public void reportProgress(final ProgressInfo progressInfo) {
				LOGGER.info(progressInfo);
			}

			@Override
			public ParentLog getParentLog() {
				return new SimpleParentLog();
			}

			@Override
			public void reportSuccess() {
				Assert.assertTrue(mascotOut.length() > 0, "Mascot result file is empty.");
			}

			@Override
			public void reportFailure(final Throwable t) {
				throw new MprcException("Mascot worker failed to process work packet.", t);
			}
		});
	}

	private String createMascotParams() {
		final ParamsInfo paramsInfo = TestMascotMappings.getAbstractParamsInfo();
		final MascotMappingFactory factory = new MascotMappingFactory();
		factory.setParamsInfo(paramsInfo);
		final Mappings mapping = factory.createMapping();
		final MappingContext context = new TestMappingContextBase(paramsInfo);

		mapping.read(mapping.baseSettings());
		mapping.setSequenceDatabase(context, TEST_MASCOT_DB);

		StringWriter writer = new StringWriter(100);
		mapping.write(mapping.baseSettings(), writer);

		return writer.toString();
	}
}
