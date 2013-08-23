package edu.mayo.mprc.xtandem;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.WorkPacketBase;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.MassUnit;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.TestMappingContextBase;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class TestTandem {
	private static final Logger LOGGER = Logger.getLogger(TestTandem.class);

	private static File tempRootDir;

	private File tandemTemp;

	private static final String DATABASE_SHORT_NAME = "SprotYeast";
	private File inputMgfFolder;
	private File inputMgfFile;
	private File fastaFolder;
	private File fastaFile;
	private File tandemInstallFolder;
	private static final XTandemMappingFactory mappingFactory = new XTandemMappingFactory();

	@BeforeClass()
	public void setup() throws IOException {
		tempRootDir = FileUtilities.createTempFolder();
		inputMgfFolder = Installer.mgfFiles(null, Installer.Action.INSTALL);
		inputMgfFile = new File(inputMgfFolder, "test.mgf");
		fastaFolder = Installer.yeastFastaFiles(null, Installer.Action.INSTALL);
		fastaFile = new File(fastaFolder, DATABASE_SHORT_NAME + ".fasta");
		tandemInstallFolder = Installer.tandem(null, Installer.Action.INSTALL);
	}

	@AfterClass()
	public void cleanup() {
		FileUtilities.cleanupTempFile(tempRootDir);
		Installer.mgfFiles(inputMgfFolder, Installer.Action.UNINSTALL);
		Installer.yeastFastaFiles(fastaFolder, Installer.Action.UNINSTALL);
		if (tandemInstallFolder != null) {
			Installer.tandem(tandemInstallFolder, Installer.Action.UNINSTALL);
		}
	}

	@Test
	public void runTandemWorker() {
		if (tandemInstallFolder == null) {
			return;
		}
		try {
			tandemTemp = new File(tempRootDir, "tandem");
			FileUtilities.ensureFolderExists(tandemTemp);

			final File tandemOut = new File(tandemTemp, "out");
			FileUtilities.ensureFolderExists(tandemOut);


			final File tandemParamFile = getTandemParams();

			String tandemExecutable = new File(tandemInstallFolder, "tandem.exe").getAbsolutePath();

			if (!new File(tandemExecutable).exists()) {
				LOGGER.warn("Could not find tandem executable in " + tandemExecutable + ", trying Tandem on the path.");
				tandemExecutable = "tandem.exe";
			}

			final XTandemWorker.Config tandemConfig = new XTandemWorker.Config(tandemExecutable);

			final XTandemWorker.Factory factory = new XTandemWorker.Factory();
			final Worker worker = factory.create(tandemConfig, null);

			final File resultFile = new File(tandemOut, "tandemResult.xml");

			final XTandemWorkPacket workPacket = new XTandemWorkPacket(inputMgfFile, tandemParamFile, resultFile, tandemOut, fastaFile, false, "0", false);
			WorkPacketBase.simulateTransfer(workPacket);

			worker.processRequest(workPacket, new ProgressReporter() {
				@Override
				public void reportStart(final String hostString) {
					LOGGER.info("Started processing " + hostString);
				}

				@Override
				public void reportProgress(final ProgressInfo progressInfo) {
					LOGGER.info(progressInfo);
				}

				@Override
				public void reportSuccess() {
					Assert.assertTrue(resultFile.length() > 0, "Tandem result file is empty.");
				}

				@Override
				public void reportFailure(final Throwable t) {
					throw new MprcException("Tandem worker failed to process work packet.", t);
				}
			});
		} catch (Exception e) {
			throw new MprcException("Tandem worker test failed.", e);
		} finally {
			FileUtilities.cleanupTempFile(tandemTemp);
		}
	}

	/**
	 * The mappings object should not fail to be created.
	 */
	@Test
	public void shouldCreateCorrectMappings() {
		createTestMappings(mappingFactory);
	}

	private File getTandemParams() throws IOException {
		final Mappings mapping = createTestMappings(mappingFactory);

		final File paramFile = new File(tandemTemp, mappingFactory.getCanonicalParamFileName(""));
		mapping.write(mapping.baseSettings(), Files.newWriter(paramFile, Charsets.UTF_8));

		return paramFile;
	}

	private Mappings createTestMappings(final XTandemMappingFactory mappingFactory) {
		final Mappings mapping = mappingFactory.createMapping();

		final MappingContext context = new TestMappingContextBase(new MockParamsInfo()) {
			@Override
			public void reportError(final String message, final Throwable t) {
				Assert.fail(message, t);
			}

			@Override
			public void reportWarning(final String message) {
				Assert.fail(message);
			}
		};

		// TODO: Excercise all mappings
		mapping.setProtease(context, new Protease("Trypsin (allow P)", "KR", ""));
		mapping.setInstrument(context, Instrument.ORBITRAP);
		mapping.setMissedCleavages(context, 2);
		mapping.setPeptideTolerance(context, new Tolerance(10, MassUnit.Ppm));
		mapping.setFragmentTolerance(context, new Tolerance(0.5, MassUnit.Da));
		return mapping;
	}

}
