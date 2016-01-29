package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.daemon.files.FileDownloader;
import edu.mayo.mprc.daemon.files.FileToken;
import edu.mayo.mprc.daemon.transfer.FileTransferChunk;
import edu.mayo.mprc.daemon.transfer.FileTransferWorkPacket;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;

/**
 * @author Roman Zenka
 */
public final class FileDownloaderImpl implements FileDownloader {
	private static final Logger LOGGER = Logger.getLogger(FileDownloaderImpl.class);

	private DaemonConnectionFactory factory;
	private DependencyResolver resolver;

	public FileDownloaderImpl(final DaemonConnectionFactory factory, final DependencyResolver resolver) {
		this.factory = factory;
		this.resolver = resolver;
	}

	@Override
	public File downloadFile(final FileToken fileToken, final File result) {
		LOGGER.debug("Downloading file from " + fileToken.toString() + " into " + result.getAbsolutePath());
		FileUtilities.ensureFolderExists(result.getParentFile());

		try {
			final DaemonConnection d = getDaemonConnection(fileToken);
			final FileDownloadListener listener = new FileDownloadListener(result);
			d.sendWork(new FileTransferWorkPacket(false, fileToken), listener);
			listener.waitUntilDownloaded();
			LOGGER.debug("Downloaded file from " + fileToken.toString() + " into " + result.getAbsolutePath());
			if (listener.getFailure() != null) {
				throw listener.getFailure();
			}
		} catch (final Exception e) {
			throw new MprcException("Failed to write chunks of file", e);
		}
		return result;
	}

	private DaemonConnection getDaemonConnection(final FileToken fileToken) {
		return factory.create(Daemon.getFileTransferServiceConfig(fileToken.getSourceDaemonConfigInfo().getDaemonId()), resolver);
	}

	private static class FileDownloadListener implements ProgressListener {
		private final File output;
		private RandomAccessFile file;
		private final CountDownLatch latch;
		private final Object lock = new Object();
		private Exception failure = null;

		public FileDownloadListener(final File output) {
			this.output = output;
			this.latch = new CountDownLatch(1);
		}

		@Override
		public void requestEnqueued(final String hostString) {

		}

		@Override
		public void requestProcessingStarted(final String hostString) {
			init();
		}

		private void init() {
			if (file != null) {
				return;
			}
			try {
				file = new RandomAccessFile(output, "rw");
				file.setLength(0);
			} catch (final Exception e) {
				throw new MprcException("Could not open " + output.getAbsolutePath(), e);
			}
		}

		public void waitUntilDownloaded() {
			try {
				latch.await();
			} catch (final InterruptedException e) {
				throw new MprcException("Download waiting interrupted", e);
			}
		}

		private void done() {
			FileUtilities.closeQuietly(file);
			latch.countDown();
		}

		public Exception getFailure() {
			synchronized (lock) {
				return failure;
			}
		}

		public void setFailure(final Exception failure) {
			synchronized (lock) {
				this.failure = failure;
			}
		}

		@Override
		public void requestProcessingFinished() {
			done();
		}

		@Override
		public void requestTerminated(final Exception e) {
			done();
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			if (progressInfo instanceof NewLogFiles) {
				// Do nothing
				return;
			}
			if (progressInfo instanceof FileTransferChunk) {
				final FileTransferChunk chunk = (FileTransferChunk) progressInfo;
				try {
					init();
					file.seek(chunk.getOffset());
					file.write(chunk.getChunk());
				} catch (final Exception e) {
					final MprcException mprcException = new MprcException("Could not write chunk to file", e);
					setFailure(e);
					throw mprcException;
				}
			}
		}
	}
}
