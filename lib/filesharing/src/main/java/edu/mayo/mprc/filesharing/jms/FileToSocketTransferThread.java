package edu.mayo.mprc.filesharing.jms;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class writes file content to socket. Once transfer is completed, socket is closed.
 */
final class FileToSocketTransferThread extends FileTransferThread {

	private static final Logger LOGGER = Logger.getLogger(FileToSocketTransferThread.class);

	private static AtomicLong lastUniqueId = new AtomicLong(0);

	private File file;
	private Socket socket;
	private long uniqueId;

	public FileToSocketTransferThread(final File file, final Socket socket) {
		super("FileToSocketTransfer: " + file.getAbsolutePath());
		this.file = file;
		this.socket = socket;

		uniqueId = lastUniqueId.incrementAndGet();
	}

	public TransferCompleteListener getTransferCompleteListener() {
		return listener;
	}

	public void setTransferCompleteListener(final TransferCompleteListener listener) {
		this.listener = listener;
	}

	public void run() {
		Exception exception = null;
		OutputStream os = null;

		LOGGER.debug("Starting to transfer data from file [" + file.getAbsolutePath() + "] to socket [" + socket.toString() + "]. Thread id: " + uniqueId);

		try {
			os = socket.getOutputStream();
			ByteStreams.copy(Files.newInputStreamSupplier(file), os);
		} catch (IOException e) {
			exception = new MprcException("File to socket data transfer failed. File [" + file.getAbsolutePath() + "].", e);
		} finally {
			FileUtilities.closeQuietly(os);
			FileUtilities.closeObjectQuietly(socket);

			if (listener != null) {
				listener.transferCompleted(new TransferCompleteEvent(file, exception));
			}
		}
	}
}
