package edu.mayo.mprc.omssa;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.WorkPacketBase;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

public class TestOmssaWorker {
	private static final Logger LOGGER = Logger.getLogger(TestOmssaWorker.class);

	private static final String SWIFT_INSTALL_ROOT_PATH = "../swift";
	private static final String DATABASE_DEPLOYMENT_DIR = SWIFT_INSTALL_ROOT_PATH + "/install/swift/var/fasta/";

	private static final String DATABASE_SHORT_NAME = "SprotYeast";

	//Omssa
	private File omssaTemp;
	private File omssaDeployedFile; //Todo: Omssa deployer, does not deploys database to the fasta directory in the Swift installation file tree. Chnage deployer to do so.

	@BeforeClass
	public void setup() {
		omssaTemp = FileUtilities.createTempFolder();
	}

	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(omssaTemp);
	}

	@Test
	public void runOmssaDeployer() {

		final File formatdbFolder = Installer.formatDb(null, Installer.Action.INSTALL);
		final File yeastFolder = Installer.yeastFastaFiles(null, Installer.Action.INSTALL);

		try {
			final File fastaFile = new File(yeastFolder, DATABASE_SHORT_NAME + ".fasta");

			omssaDeployedFile = new File(DATABASE_DEPLOYMENT_DIR, DATABASE_SHORT_NAME + "/" + fastaFile.getName());

			final String formatdbPath = new File(formatdbFolder, FileUtilities.isWindowsPlatform() ? "formatdb.exe" : "formatdb").getAbsolutePath();
			final OmssaDeploymentService.Config omssaConfig = new OmssaDeploymentService.Config(formatdbPath, omssaTemp.getAbsolutePath());

			final OmssaDeploymentService.Factory factory = new OmssaDeploymentService.Factory();

			final OmssaDeploymentService deploymentService = (OmssaDeploymentService) factory.create(omssaConfig, null);

			FileUtilities.ensureFolderExists(deploymentService.getDeployableDbFolder());

			final Curation curation = new Curation();
			curation.setShortName(DATABASE_SHORT_NAME);
			curation.setCurationFile(fastaFile);

			final DeploymentRequest request = new DeploymentRequest("0", curation.getFastaFile());
			WorkPacketBase.simulateTransfer(request);
			final DeploymentResult result = deploymentService.performDeployment(request);
			WorkPacketBase.simulateTransfer(result);

			omssaDeployedFile = result.getDeployedFile();

			Assert.assertTrue(omssaDeployedFile.exists(), "Database file was not deployed.");
			Assert.assertTrue(omssaDeployedFile.getParentFile().listFiles().length > 1, "Omssa database index files were not created.");
		} catch (Exception e) {
			throw new MprcException("Omssa deployment service test failed.", e);
		} finally {
			Installer.yeastFastaFiles(yeastFolder, Installer.Action.UNINSTALL);
			Installer.formatDb(formatdbFolder, Installer.Action.UNINSTALL);
		}
	}

	@Test(dependsOnMethods = {"runOmssaDeployer"})
	public void runOmssaWorker() throws IOException {
		final File omssaFolder = Installer.omssa(null, Installer.Action.INSTALL);
		final File mgfFolder = Installer.mgfFiles(null, Installer.Action.INSTALL);
		try {
			final File omssaOut = new File(omssaTemp, "omssa.out");
			final File inputMgfFile = new File(mgfFolder, "test.mgf");

			final File omssaParamFile = makeParamsFile();

			String omssaclPath = null;

			if (FileUtilities.isWindowsPlatform()) {
				omssaclPath = new File(omssaFolder, "omssacl.exe").getAbsolutePath();
			} else if (FileUtilities.isLinuxPlatform()) {
				omssaclPath = new File(omssaFolder, "omssacl").getAbsolutePath();
			} else {
				throw new MprcException("Unsupported platform");
			}

			final OmssaWorker.Config omssaConfig = new OmssaWorker.Config(omssaclPath);
			final OmssaWorker.Factory factory = new OmssaWorker.Factory();
			factory.setOmssaUserModsWriter(new OmssaUserModsWriter());

			final OmssaWorker omssaWorker = (OmssaWorker) factory.create(omssaConfig, null);

			final OmssaWorkPacket workPacket = new OmssaWorkPacket(omssaOut, omssaParamFile, inputMgfFile, omssaDeployedFile, new LinkedList<File>(), false, "0", false);
			WorkPacketBase.simulateTransfer(workPacket);

			omssaWorker.processRequest(workPacket, new ProgressReporter() {
				@Override
				public void reportStart(final String hostString) {
					LOGGER.info("Started processing on "+hostString);
				}

				@Override
				public void reportProgress(final ProgressInfo progressInfo) {
					LOGGER.info(progressInfo);
				}

				@Override
				public void reportSuccess() {
					Assert.assertTrue(omssaOut.length() > 0, "Omssa result file is empty.");
				}

				@Override
				public void reportFailure(final Throwable t) {
					throw new MprcException("Omssa worker failed to process work packet.", t);
				}
			});
		} finally {
			Installer.omssa(omssaFolder, Installer.Action.UNINSTALL);
			Installer.mgfFiles(mgfFolder, Installer.Action.UNINSTALL);
		}
	}

	private File makeParamsFile() throws IOException {
		final OmssaMappingFactory mappingFactory = new OmssaMappingFactory();
		final Mappings mapping = mappingFactory.createMapping();
		final Reader isr = mapping.baseSettings();
		mapping.read(isr);
		FileUtilities.closeQuietly(isr);

		final File omssaParamFile = new File(omssaTemp, mappingFactory.getCanonicalParamFileName(""));

		final BufferedWriter writer = Files.newWriter(omssaParamFile, Charsets.UTF_8);
		final Reader oldParams = mapping.baseSettings();
		mapping.write(oldParams, writer);
		FileUtilities.closeQuietly(oldParams);
		FileUtilities.closeQuietly(writer);
		return omssaParamFile;
	}

}
