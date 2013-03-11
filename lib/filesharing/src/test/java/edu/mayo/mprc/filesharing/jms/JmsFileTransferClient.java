package edu.mayo.mprc.filesharing.jms;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.filesharing.FileTransfer;
import edu.mayo.mprc.filesharing.FileTransferHandler;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test class.
 */
public final class JmsFileTransferClient {
	private JmsFileTransferClient() {
	}

	public static void main(final String[] args) {
		if (args.length >= 3) {
			final String brokerURl = args[0];
			final String remoteFilePath = args[1];
			final File localFile = new File(args[2]);

			final URI brokerUri;
			try {
				brokerUri = new URI(brokerURl);
			} catch (URISyntaxException e) {
				throw new MprcException("Cannot parse broker URL ["+brokerURl+"]", e);
			}
			final JmsFileTransferHandlerFactory factory = new JmsFileTransferHandlerFactory(brokerUri, null, null);
			final FileTransferHandler fileSharing = factory.createFileSharing("client");
			fileSharing.startProcessingRequests();

			final long startTime = System.currentTimeMillis();

			final FileTransfer fileTransfer = fileSharing.getFile("server", remoteFilePath, localFile);
			final File result = fileTransfer.done().get(0);

			final long finishTime = System.currentTimeMillis();

			if (result.length() > 0) {
				FileUtilities.out("File transfer succeeded.");
				FileUtilities.out("Transfer of " + result.length() + " bytes took " + (finishTime - startTime) / 1000 + " seconds.");
			} else {
				FileUtilities.out("File transfer failed.");
				fileTransfer.getErrorException().printStackTrace();
			}

			System.exit(0);
		} else {
			FileUtilities.err("Usage: java -jar filesharing.jar edu.mayo.mprc.filesharing.jms.JmsFileTransferClient <brokerURI> <remoteFilePath> <localFilePath>");
			System.exit(1);
		}
	}
}
