package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerBase;
import edu.mayo.mprc.daemon.worker.WorkerFactoryBase;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ScaffoldReportWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(ScaffoldReportWorker.class);
	public static final String TYPE = "scaffoldReport";
	public static final String NAME = "Scaffold Report";
	public static final String DESC = "Automatically exports an excel peptide report from Scaffold. Useful if you want to provide reports to customers unable or unwilling to use Scaffold. Requires 2.2.03 or newer version of Scaffold Batch.";

	/**
	 * Null Constructor
	 */
	public ScaffoldReportWorker() {
	}

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter progressReporter) {
		if (workPacket instanceof ScaffoldReportWorkPacket) {

			final ScaffoldReportWorkPacket scaffoldReportWorkPacket = ScaffoldReportWorkPacket.class.cast(workPacket);

			final File finalPeptideReport = scaffoldReportWorkPacket.getPeptideReportFile();
			final File finalProteinReport = scaffoldReportWorkPacket.getProteinReportFile();
			final File peptideReport = getTempOutputFile(tempWorkFolder, finalPeptideReport);
			final File proteinReport = getTempOutputFile(tempWorkFolder, finalProteinReport);

			if (finalPeptideReport.exists() && finalPeptideReport.length() > 0 && finalProteinReport.exists() && finalProteinReport.length() > 0) {
				LOGGER.info("Scaffold report output files: " + finalPeptideReport.getName() + " and " + finalProteinReport.getName() + " already exist. Skipping scaffold report generation.");
				return;
			}

			final List<File> fileArrayList = new ArrayList<File>(scaffoldReportWorkPacket.getScaffoldOutputFiles().size());

			for (final File file : scaffoldReportWorkPacket.getScaffoldOutputFiles()) {
				fileArrayList.add(file);
			}

			try {
				ScaffoldReportBuilder.buildReport(fileArrayList, peptideReport, proteinReport);
				publish(peptideReport, finalPeptideReport);
				publish(proteinReport, finalProteinReport);
			} catch (IOException e) {
				throw new MprcException("Failed to process scaffold report work packet.", e);
			}

		} else {
			throw new MprcException("Failed to process scaffold report work packet, expecting type " +
					ScaffoldReportWorkPacket.class.getName() + " instead of " + workPacket.getClass().getName());
		}
	}


	/**
	 * A factory capable of creating the worker
	 */
	@Component("scaffoldReportWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new ScaffoldReportWorker();
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			// No UI needed
		}
	}
}
