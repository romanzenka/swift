package edu.mayo.mprc.scaffold.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		if (workPacket instanceof ScaffoldReportWorkPacket) {

			final ScaffoldReportWorkPacket scaffoldReportWorkPacket = ScaffoldReportWorkPacket.class.cast(workPacket);

			final File peptideReport = scaffoldReportWorkPacket.getPeptideReportFile();
			final File proteinReport = scaffoldReportWorkPacket.getProteinReportFile();

			if (peptideReport.exists() && peptideReport.length() > 0 && proteinReport.exists() && proteinReport.length() > 0) {
				LOGGER.info("Scaffold report output files: " + peptideReport.getName() + " and " + proteinReport.getName() + " already exist. Skipping scaffold report generation.");
				return;
			}

			final List<File> fileArrayList = new ArrayList<File>(scaffoldReportWorkPacket.getScaffoldOutputFiles().size());

			for (final File file : scaffoldReportWorkPacket.getScaffoldOutputFiles()) {
				fileArrayList.add(file);
			}

			try {
				ScaffoldReportBuilder.buildReport(fileArrayList, peptideReport, proteinReport);
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
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new ScaffoldReportWorker();
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {

		public Config() {
		}

		@Override
		public Map<String, String> save(final DependencyResolver resolver) {
			return new TreeMap<String, String>();
		}

		@Override
		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			//Do nothing
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			// No UI needed
		}
	}
}
