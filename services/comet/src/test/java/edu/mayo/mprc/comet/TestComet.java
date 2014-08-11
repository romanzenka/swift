package edu.mayo.mprc.comet;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.worker.WorkPacketBase;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
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

/**
 * @author Roman Zenka
 */
public final class TestComet {
	private static final Logger LOGGER = Logger.getLogger(TestComet.class);

	private static File tempRootDir;

	private File cometTemp;

	private static final String DATABASE_SHORT_NAME = "SprotYeast";
	private File inputMzmlFolder;
	private File inputMzmlFile;
	private File fastaFolder;
	private File fastaFile;
	private File cometExecutable;
	private static final CometMappingFactory mappingFactory = new CometMappingFactory();

	@BeforeClass()
	public void setup() throws IOException {
		cometExecutable = Installer.getExecutable("SWIFT_TEST_COMET_EXECUTABLE", "comet executable");
		tempRootDir = FileUtilities.createTempFolder();
		inputMzmlFolder = Installer.mzmlFiles(null, Installer.Action.INSTALL);
		inputMzmlFile = new File(inputMzmlFolder, "test.mzML");
		fastaFolder = Installer.yeastFastaFiles(null, Installer.Action.INSTALL);
		fastaFile = new File(fastaFolder, DATABASE_SHORT_NAME + ".fasta");
	}

	@AfterClass()
	public void cleanup() {
		FileUtilities.cleanupTempFile(tempRootDir);
		Installer.mzmlFiles(inputMzmlFolder, Installer.Action.UNINSTALL);
		Installer.yeastFastaFiles(fastaFolder, Installer.Action.UNINSTALL);
	}

	@Test
	public void runCometWorker() {
		if (cometExecutable == null) {
			return;
		}
		try {
			cometTemp = new File(tempRootDir, "comet");
			FileUtilities.ensureFolderExists(cometTemp);

			final File cometOut = new File(cometTemp, "out");
			FileUtilities.ensureFolderExists(cometOut);

			final String cometParams = getCometParams();

			String cometExecutable = this.cometExecutable.getAbsolutePath();

			if (!new File(cometExecutable).exists()) {
				LOGGER.warn("Could not find comet executable in " + cometExecutable + ", trying Comet on the path.");
				cometExecutable = "comet.exe";
			}

			final CometWorker.Config cometConfig = new CometWorker.Config();
			cometConfig.put(CometWorker.COMET_EXECUTABLE, cometExecutable);

			final CometWorker.Factory factory = new CometWorker.Factory();
			final Worker worker = factory.create(cometConfig, null);

			final File resultFile = new File(cometOut, "result.pep.xml");

			final CometWorkPacket workPacket = new CometWorkPacket(inputMzmlFile, cometParams, resultFile, fastaFile, false, false);
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
				public ParentLog getLog() {
					return new SimpleParentLog();
				}

				@Override
				public void reportSuccess() {
					Assert.assertTrue(resultFile.length() > 70000, "Comet result file is too small.");
				}

				@Override
				public void reportFailure(final Throwable t) {
					throw new MprcException("Comet worker failed to process work packet.", t);
				}
			});
		} catch (Exception e) {
			throw new MprcException("Comet worker test failed.", e);
		} finally {
			FileUtilities.cleanupTempFile(cometTemp);
		}
	}

	/**
	 * The mappings object should not fail to be created.
	 */
	@Test
	public void shouldCreateCorrectMappings() {
		createTestMappings(mappingFactory);
	}

	private String getCometParams() {
		final Mappings mapping = createTestMappings(mappingFactory);

		StringWriter writer = new StringWriter(100);
		mapping.write(mapping.baseSettings(), writer);

		return writer.toString();
	}

	private Mappings createTestMappings(final CometMappingFactory mappingFactory) {
		final Mappings mapping = mappingFactory.createMapping();

		final MappingContext context = new TestMappingContextBase(new MockParamsInfo()) {
			@Override
			public void reportError(final String message, final Throwable t, ParamName paramName) {
				Assert.fail(message, t);
			}

			@Override
			public void reportWarning(final String message, ParamName paramName) {
				Assert.fail(message);
			}
		};

		// TODO: Excercise all mappings
		mapping.setProtease(context, new Protease("Trypsin (allow P)", "KR", ""));
		mapping.setInstrument(context, Instrument.ORBITRAP);
		mapping.setMinTerminiCleavages(context, 2);
		mapping.setMissedCleavages(context, 2);
		mapping.setPeptideTolerance(context, new Tolerance(10, MassUnit.Ppm));
		mapping.setFragmentTolerance(context, new Tolerance(0.5, MassUnit.Da));
		return mapping;
	}

}
