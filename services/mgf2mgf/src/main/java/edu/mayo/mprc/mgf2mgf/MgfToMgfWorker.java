package edu.mayo.mprc.mgf2mgf;

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
import edu.mayo.mprc.io.mgf.MgfCleanup;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.springframework.stereotype.Component;

import java.io.File;

public final class MgfToMgfWorker extends WorkerBase {

	public static final String TYPE = "mgf2mgf";
	public static final String NAME = "Mgf Cleanup";
	public static final String DESC = "Swift expects <tt>.mgf</tt> headers to be in certain format (indicate the spectrum), so the results of the search engines can be more easily pieced together. If you want to search .mgf files directly, the cleaner has to check that the headers are okay and modify them if they are not. Without this module, Swift cannot process <tt>.mgf</tt> files.";

	@Override
	public void process(final WorkPacket wp, final File tempWorkFolder, final UserProgressReporter reporter) {
		final MgfTitleCleanupWorkPacket workPacket = (MgfTitleCleanupWorkPacket) wp;
		final File mgfFile = workPacket.getMgfToCleanup();
		final File outCleanedMgf = workPacket.getCleanedMgf();
		final File cleanedMgf = getTempOutputFile(tempWorkFolder, outCleanedMgf);

		boolean cleanupNeeded = false;
		// If we do not have a file, or it is too old
		if (!outCleanedMgf.exists() || outCleanedMgf.lastModified() < mgfFile.lastModified()) {
			cleanupNeeded = new MgfCleanup(mgfFile).produceCleanedMgf(cleanedMgf);
			publish(cleanedMgf, outCleanedMgf);
		} else {
			// The mgf is already there, therefore it must have been cleaned before, therefore cleanup WAS needed.
			// We must return true, otherwise the caller would use the mgfFile instead of cleanedMgf, although we
			// technically did NOT need to perform a cleanup.
			cleanupNeeded = true;
		}
		// Report whether we did perform the cleanup
		reporter.reportProgress(new MgfTitleCleanupResult(cleanupNeeded));
	}


	public String toString() {
		return "Mgf Title Cleanup";
	}

	@Override
	public String check() {
		// This worker should have no problems running no matter what
		return null;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("mgfToMgfWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			return new MgfToMgfWorker();
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
		}
	}
}
