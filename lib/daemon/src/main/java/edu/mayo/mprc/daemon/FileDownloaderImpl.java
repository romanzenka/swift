package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.daemon.files.FileDownloader;
import edu.mayo.mprc.daemon.files.FileToken;
import edu.mayo.mprc.daemon.files.FileTransferChunk;
import edu.mayo.mprc.daemon.files.FileTransferWorkPacket;
import edu.mayo.mprc.daemon.worker.log.NewLogFiles;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * @author Roman Zenka
 */
public final class FileDownloaderImpl implements FileDownloader {
	private DaemonConnectionFactory factory;
	private DependencyResolver resolver;

	public FileDownloaderImpl(DaemonConnectionFactory factory, DependencyResolver resolver) {
		this.factory = factory;
		this.resolver = resolver;
	}

	@Override
	public File actuallyDownloadFile(final FileToken fileToken, File result) {
		FileUtilities.ensureFolderExists(result.getParentFile());

		try {
			DaemonConnection d = getDaemonConnection(fileToken);
			d.sendWork(new FileTransferWorkPacket(false, fileToken),
					new FileDownloadListener(result));
		} catch (Exception e) {
			throw new MprcException("Failed to write chunks of file", e);
		}
		return result;
	}

	private DaemonConnection getDaemonConnection(FileToken fileToken) {
		return factory.create(Daemon.getFileTransferServiceConfig(fileToken.getSourceDaemonConfigInfo().getDaemonId()), resolver);
	}

	private static class FileDownloadListener implements ProgressListener {
		private final File output;
		private RandomAccessFile file;

		public FileDownloadListener(File output) {
			this.output = output;
		}

		@Override
		public void requestEnqueued(String hostString) {

		}

		@Override
		public void requestProcessingStarted(String hostString) {
			init();
		}

		private void init() {
			if (file != null) {
				return;
			}
			try {
				file = new RandomAccessFile(output, "rw");
				file.setLength(0);
			} catch (Exception e) {
				throw new MprcException("Could not open " + output.getAbsolutePath(), e);
			}
		}

		@Override
		public void requestProcessingFinished() {
			FileUtilities.closeQuietly(file);
		}

		@Override
		public void requestTerminated(Exception e) {
			FileUtilities.closeQuietly(file);
		}

		@Override
		public void userProgressInformation(ProgressInfo progressInfo) {
			if (progressInfo instanceof NewLogFiles) {
				// Do nothing
				return;
			}
			if (progressInfo instanceof FileTransferChunk) {
				FileTransferChunk chunk = (FileTransferChunk) progressInfo;
				try {
					init();
					file.seek(chunk.getOffset());
					file.write(chunk.getChunk());
				} catch (Exception e) {
					throw new MprcException("Could not write chunk to file", e);
				}
			} else {
				ExceptionUtilities.throwCastException(progressInfo, FileTransferChunk.class);
			}
		}
	}
}
