package edu.mayo.mprc.filesharing.jms;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class writes data streaming from socket to file. Once transfer is completed, socket is closed.
 */
final class SocketToFileTransferThread extends FileTransferThread {

	private static AtomicLong lastUniqueId = new AtomicLong(0);

	private static final Logger LOGGER = Logger.getLogger(SocketToFileTransferThread.class);

	private FileInfo fileInfo;
	private Socket socket;
	private long uniqueId;

	public SocketToFileTransferThread(final FileInfo fileInfo, final Socket socket) {
		super("SocketToFileTransfer: " + fileInfo.getFilePath());
		this.fileInfo = fileInfo;
		this.socket = socket;

		uniqueId = lastUniqueId.incrementAndGet();
	}

	public void run() {
		Exception exception = null;
		InputStream is = null;
		final File file = new File(fileInfo.getFilePath());

		LOGGER.debug("Starting to transfer data from socket [" + socket.toString() + "] to file [" + fileInfo.getFilePath() + "]. Thread id: " + uniqueId);

		try {
			is = socket.getInputStream();

			FileUtilities.ensureFolderExists(file.getParentFile());

			FileUtilities.writeStreamToFile(is, file);

			FileUtilities.setLastModified(file, fileInfo.getLastModified());

			// The file must not shrink, we expect the files to only grow in size
			if (file.length() < fileInfo.getLength()) {
				exception = new MprcException("Transfered file [" + file.getAbsolutePath() + "] size " + file.length() + " is excepted to be " + fileInfo.getLength() + " bytes.");
			}
		} catch (IOException e) {
			exception = new MprcException("Socket to fileInfo data transfer failed. File [" + fileInfo.getFilePath() + "].", e);
		} finally {
			FileUtilities.closeQuietly(is);
			FileUtilities.closeObjectQuietly(socket);

			if (getTransferCompleteListener() != null) {
				getTransferCompleteListener().transferCompleted(new TransferCompleteEvent(file, exception));
			}
		}
	}
}
