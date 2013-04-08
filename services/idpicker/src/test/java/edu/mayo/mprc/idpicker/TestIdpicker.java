package edu.mayo.mprc.idpicker;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Requires idpqonvert to be present at a path obtained from the environment variable
 * <code>edu.mayo.mprc.swift.idpicker.path</code>
 * <p/>
 * If not available, the tests are skipped
 *
 * @author Roman Zenka
 */
public final class TestIdpicker {
	private static final Logger LOGGER = Logger.getLogger(TestIdpicker.class);
	private File idpQonvert;
	private File tmpFolder;
	private File pepXmlFile;
	private File fastaFile;

	@BeforeClass
	public void startup() {
		if (FileUtilities.isLinuxPlatform() || FileUtilities.isWindowsPlatform()) {
			final String path = System.getenv("edu.mayo.mprc.swift.idpicker.path");
			if (path == null) {
				throw new MprcException("Skipping IdpQonvert tests");
			}
			idpQonvert = new File(path).getAbsoluteFile();
			if (!idpQonvert.isFile() || !idpQonvert.canExecute()) {
				throw new MprcException("Cannot execute idpqonvert: [" + idpQonvert.getAbsolutePath() + "]");
			}

			tmpFolder = FileUtilities.createTempFolder();
			final File pepXmlFolder = Installer.pepXmlFiles(tmpFolder, Installer.Action.INSTALL);
			final File fastaFolder = Installer.yeastFastaFiles(tmpFolder, Installer.Action.INSTALL);
			pepXmlFile = new File(pepXmlFolder, "test.pepXML");
			fastaFile = new File(fastaFolder, "SprotYeast.fasta");
			if (!pepXmlFile.isFile()) {
				throw new MprcException("Failed to create test pepXML file");
			}
		}
	}

	@AfterClass
	public void teardown() {
		if (FileUtilities.isLinuxPlatform() || FileUtilities.isWindowsPlatform()) {
			FileUtilities.cleanupTempFile(tmpFolder);
			Installer.pepXmlFiles(tmpFolder, Installer.Action.UNINSTALL);
			Installer.yeastFastaFiles(tmpFolder, Installer.Action.UNINSTALL);
		}
	}

	@Test
	public void shouldRun() {
		if (FileUtilities.isLinuxPlatform() || FileUtilities.isWindowsPlatform()) {
			IdpickerWorker.Config config = new IdpickerWorker.Config();
			config.setIdpQonvertExecutable(idpQonvert.getAbsolutePath());

			IdpickerWorker.Factory factory = new IdpickerWorker.Factory();
			factory.setConfig(config);
			final IdpickerWorker worker = (IdpickerWorker) factory.createWorker();

			final File outputFile = tempFile("out.idp");
			final File inputFile = pepXmlFile;
			final IdpQonvertSettings params = new IdpQonvertSettings();
			params.setDecoyPrefix("REVERSE_");

			final IdpickerWorkPacket workPacket = new IdpickerWorkPacket(outputFile, params, inputFile, fastaFile, "idp-test", true);
			worker.process(workPacket, new UserProgressReporter() {
				@Override
				public void reportProgress(final ProgressInfo progressInfo) {
					LOGGER.debug(progressInfo.toString());
				}
			});
		}
	}

	private File tempFile(final String fileName) {
		return new File(tmpFolder, fileName);
	}
}
