package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DependencyResolver;
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
 * {@code edu.mayo.mprc.swift.idpqonvert.path}
 * <p/>
 * If not available, the tests are skipped
 *
 * @author Roman Zenka
 */
public final class TestIdpQonvert {
	private static final Logger LOGGER = Logger.getLogger(TestIdpQonvert.class);
	private File idpQonvert;
	private File tmpFolder;
	private File pepXmlFile;
	private File fastaFile;
	private File outputFile;
	private File pepXmlFolder;
	private File fastaFolder;

	@BeforeClass
	public void startup() {
		if (FileUtilities.isLinuxPlatform() || FileUtilities.isWindowsPlatform()) {
			idpQonvert = Installer.getExecutable("SWIFT_TEST_IDPQONVERT", "idpqonvert executable");
			idpQonvert = idpQonvert.getAbsoluteFile();

			tmpFolder = FileUtilities.createTempFolder();
			pepXmlFolder = Installer.pepXmlFiles(new File(tmpFolder, "pepXML"), Installer.Action.INSTALL);
			fastaFolder = Installer.yeastFastaFiles(new File(tmpFolder, "fasta"), Installer.Action.INSTALL);
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
			FileUtilities.cleanupTempFile(new File(fastaFolder, "SprotYeast.fasta.index"));
			Installer.pepXmlFiles(pepXmlFolder, Installer.Action.UNINSTALL);
			Installer.yeastFastaFiles(fastaFolder, Installer.Action.UNINSTALL);
			FileUtilities.cleanupTempFile(outputFile);
			FileUtilities.cleanupTempFile(tmpFolder);
		}
	}

	@Test
	public void shouldRun() {
		if (FileUtilities.isLinuxPlatform() || FileUtilities.isWindowsPlatform()) {
			IdpQonvertWorker.Config config = new IdpQonvertWorker.Config();
			config.put(IdpQonvertWorker.IDPQONVERT_EXECUTABLE, idpQonvert.getAbsolutePath());

			IdpQonvertWorker.Factory factory = new IdpQonvertWorker.Factory();
			final IdpQonvertWorker worker = (IdpQonvertWorker) factory.create(config, new DependencyResolver(null));

			outputFile = tempFile("out.idp");
			final File inputFile = pepXmlFile;
			final IdpQonvertSettings params = new IdpQonvertSettings();
			params.setDecoyPrefix("REVERSE_");

			final IdpQonvertWorkPacket workPacket = new IdpQonvertWorkPacket(
					outputFile, params, inputFile, fastaFile, inputFile.getParentFile(), "idp-test", true);
			worker.process(workPacket, outputFile.getParentFile(), new UserProgressReporter() {
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
