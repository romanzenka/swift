package edu.mayo.mprc.daemon.transfer;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ResourceConfigBase;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.worker.*;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * Responds to file transfer requests.
 *
 * @author Roman Zenka
 */
public final class FileTransferWorker extends WorkerBase implements NoLoggingWorker {
	private static final Logger LOGGER = Logger.getLogger(FileTransferWorker.class);
	public static final String TYPE = "fileTransfer";
	public static final String NAME = "File Transfer Responder";
	public static final String DESC = "Responds to requests for files. Automatically set for each daemon.";
	public static final int BUFFER_SIZE = 1024 * 1024; // 1 MB buffer

	private FileTokenFactory fileTokenFactory;

	@Override
	public void process(final WorkPacket workPacket, final File tempWorkFolder, final UserProgressReporter reporter) {
		if (workPacket instanceof FileTransferWorkPacket) {
			final FileTransferWorkPacket fileTransfer = (FileTransferWorkPacket) workPacket;
			LOGGER.debug("Uploading "+((FileTransferWorkPacket) workPacket).getFileToken().toString());
			final File file = fileTokenFactory.getFile(fileTransfer.getFileToken());
			if (!file.exists()) {
				throw new MprcException("The requested file does not exist: " + file.getAbsolutePath());
			}
			final FileInputStream stream = FileUtilities.getInputStream(file);
			final byte[] buffer = new byte[BUFFER_SIZE];
			long offset = 0;
			try {
				final int numRead = stream.read(buffer);
				final byte[] chunk = buffer.length == numRead ? buffer : Arrays.copyOfRange(buffer, 0, numRead);
				reporter.reportProgress(new FileTransferChunk(chunk, offset));
				offset += numRead;
			} catch (final Exception e) {
				FileUtilities.closeQuietly(stream);
				throw new MprcException("Could not read file: " + file.getAbsolutePath());
			}
			LOGGER.debug("Uploaded "+((FileTransferWorkPacket) workPacket).getFileToken().toString());
		}
	}

	public String toString() {
		return "file transfer support";
	}

	@Override
	public File createTempWorkFolder() {
		// Needs no temp work folder
		return null;
	}

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	/**
	 * A fileTokenFactory capable of creating the worker
	 */
	@Component("fileTransferWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> {
		private FileTokenFactory fileTokenFactory;

		public FileTokenFactory getFileTokenFactory() {
			return fileTokenFactory;
		}

		@Resource(name = "fileTokenFactory")
		public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
			this.fileTokenFactory = fileTokenFactory;
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final FileTransferWorker fileTransferWorker = new FileTransferWorker();
			fileTransferWorker.setFileTokenFactory(fileTokenFactory);
			return fileTransferWorker;
		}
	}

	/**
	 * Configuration for the fileTokenFactory
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
