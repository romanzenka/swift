package edu.mayo.mprc.filesharing.jms;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.filesharing.FileTransfer;
import edu.mayo.mprc.filesharing.FileTransferHandler;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import org.apache.activemq.ActiveMQSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

/**
 * Class Connect to JMS Broker and listens for file transfer requests from remote peers. When a request is
 * completed or fails, a response is set to null if the request is completed successfully or is set to
 * an Exception object if the request fails.
 * <p/>
 * This class also handles local request for remote files. Local request requires an identifier of the file's
 * source system, a remote file path and a local destination file path.
 * <p/>
 * Multiple local and remote requests can be handle simultaneously in a thread safe manner.
 */
public final class JmsFileTransferHandler implements FileTransferHandler {

	private static final Logger LOGGER = Logger.getLogger(JmsFileTransferHandler.class);
	private ActiveMQSession session;
	private Connection connection;
	private final URI brokerURI;
	private final String queueName;
	private final String userName;
	private final String password;

	private Destination destination;
	private MessageConsumer consumer;

	private Semaphore runningSemaphore;

	private Map<String, MessageProducer> messageProducers;

	private static final String QUEUE_PREFIX = "FileTransferHandler-";

	/**
	 * Constructor blocks until the connection to JMS broker is stablished.
	 *
	 * @param broker
	 * @param sourceId
	 * @param userName
	 * @param password
	 */
	public JmsFileTransferHandler(final URI broker, final String sourceId, final String userName, final String password) {
		runningSemaphore = new Semaphore(1);
		messageProducers = Collections.synchronizedMap(new TreeMap<String, MessageProducer>());

		this.brokerURI = broker;
		this.queueName = QUEUE_PREFIX + sourceId;
		this.userName = userName;
		this.password = password;
	}

	/**
	 * Starts processing local and remote file transfer requests.
	 */
	public void startProcessingRequests() {
		startProcessingRequests(true);
	}

	public void startProcessingRequests(final boolean processRemoteRequests) {
		synchronized (runningSemaphore) {
			if (runningSemaphore.tryAcquire()) {
				try {
					connection = ActiveMQConnectionPool.getConnectionToBroker(brokerURI, userName, password);

					session = (ActiveMQSession) this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

					destination = session.createQueue(queueName);

					connection.start();

					LOGGER.info("Connected to JMS broker: " + brokerURI.toString() + " queue: " + this.queueName);

					LOGGER.info("Starting file transfer request processing for queue: " + queueName);

					final MessageConsumer consumer = getMessageConsumer();

					if (processRemoteRequests) {
						consumer.setMessageListener(new MessageListener() {
							public void onMessage(final Message message) {
								processFileTransferRequest(message);
							}
						});
					}
				} catch (Exception e) {
					runningSemaphore.release();
					throw new MprcException("Failed to start processing file transfer requests from queue: " + queueName + " at JMS broker URI: " + brokerURI, e);
				}
			}
		}
	}

	public void stopProcessingRequest() {
		synchronized (runningSemaphore) {
			if (runningSemaphore.availablePermits() == 0) {
				try {
					LOGGER.info("Stopping file transfer request processing for queue: " + queueName);
					closeConnection();
				} catch (JMSException e) {
					throw new MprcException("Error ocurred while stopping file transfer request processing for queue: " + queueName, e);
				}

				runningSemaphore.release();
			}
		}
	}

	public FileTransfer getFile(final String sourceId, final String sourcefilePath, final File localDestinationFile) {
		if (runningSemaphore.availablePermits() == 0) {
			return JmsFileTransferHelper.download(session, getMessageProducer(sourceId), sourcefilePath, localDestinationFile);
		}

		return null;
	}

	@Override
	public FileTransfer uploadFile(final String destinationId, final File localSourceFile, final String destinationFilePath) {
		if (localSourceFile.exists() && !localSourceFile.isDirectory()) {
			final Map<File, String> localFileRemoteFilePathPairs = new TreeMap<File, String>();
			localFileRemoteFilePathPairs.put(localSourceFile, destinationFilePath);

			return synchronizeRemoteFiles(destinationId, localFileRemoteFilePathPairs);
		} else if (localSourceFile.exists() && localSourceFile.isDirectory()) {
			return uploadFolder(destinationId, localSourceFile, destinationFilePath);
		}

		return new FileTransfer(localSourceFile);
	}

	@Override
	public FileTransfer uploadFolder(final String destinationId, final File localSourceFolder, final String destinationFolderPath) {
		final Map<File, String> localFileRemoteFilePathPairs = new TreeMap<File, String>();

		getFilesFromFolder(localSourceFolder, normalizedFolderPath(localSourceFolder.getAbsolutePath()), normalizedFolderPath(destinationFolderPath), localFileRemoteFilePathPairs);

		return synchronizeRemoteFiles(destinationId, localFileRemoteFilePathPairs);
	}

	@Override
	public FileTransfer downloadFile(final String sourceId, final File localDestinationFile, final String sourceFilePath) {
		final Map<File, String> localFileRemoteFilePathPairs = new TreeMap<File, String>();
		localFileRemoteFilePathPairs.put(localDestinationFile, sourceFilePath);

		return synchronizeLocalFiles(sourceId, localFileRemoteFilePathPairs);
	}

	private void getFilesFromFolder(final File localParentFolder, final String localRootFolderPath, final String destinationRootFolderPath, final Map<File, String> localFileRemoteFilePathPairs) {
		for (final File file : localParentFolder.listFiles()) {
			if (file.isDirectory()) {
				getFilesFromFolder(file, localRootFolderPath, destinationRootFolderPath, localFileRemoteFilePathPairs);
			} else {
				localFileRemoteFilePathPairs.put(file, destinationRootFolderPath + file.getAbsolutePath().substring(localRootFolderPath.length()));
			}
		}
	}

	private String normalizedFolderPath(final String folderPath) {
		if (folderPath.endsWith("/")) {
			return folderPath;
		} else {
			return folderPath + "/";
		}
	}

	private FileTransfer synchronizeRemoteFiles(final String remoteId, final Map<File, String> localFileRemoteFilePathPairs) {
		if (runningSemaphore.availablePermits() == 0) {
			return JmsFileTransferHelper.upload(session, getMessageProducer(remoteId), localFileRemoteFilePathPairs);
		}

		return null;
	}

	private FileTransfer synchronizeLocalFiles(final String remoteId, final Map<File, String> localFileRemoteFilePathPairs)  {
		if (runningSemaphore.availablePermits() == 0) {
			return JmsFileTransferHelper.download(session, getMessageProducer(remoteId), localFileRemoteFilePathPairs);
		}

		return null;
	}

	private void processFileTransferRequest(final Message message) {
		try {
			final Object object = ((ObjectMessage) message).getObject();

			if (object instanceof MultiFileTransferRequest) {
				final MultiFileTransferRequest fileTransferRequest = (MultiFileTransferRequest) object;

				LOGGER.info("File transfer request received from queue: " + queueName + " for files: " + fileTransferRequest.getFileInfos().toString());

				JmsFileTransferHelper.processMultiFileTransferRequest(fileTransferRequest, session, message.getJMSReplyTo());
			} else {
				session.createProducer(message.getJMSReplyTo()).send(session.createObjectMessage(new MprcException("Request in object message must be of the type " + MultiFileTransferRequest.class.getName())));
			}
		} catch (Exception e) {
			LOGGER.error("File transfer request for queue " + queueName + " could not be process.", e);
		}
	}

	private MessageConsumer getMessageConsumer() {
		if (consumer == null) {
			try {
				consumer = session.createConsumer(destination);
			} catch (JMSException e) {
				throw new MprcException("Cannot create consumer for destination [" + destination + "]", e);
			}
		}

		return consumer;
	}

	private void closeConnection() throws JMSException {
		messageProducers.clear();

		if (connection != null) {
			connection.close();
		}
	}

	private synchronized MessageProducer getMessageProducer(final String sourceId) {
		try {
			MessageProducer producer = null;

			if ((producer = messageProducers.get(sourceId)) == null) {
				producer = session.createProducer(session.createQueue(QUEUE_PREFIX + sourceId));
				messageProducers.put(sourceId, producer);
			}

			return producer;
		} catch (JMSException e) {
			throw new MprcException("Cannot obtain message producer for queue [" + QUEUE_PREFIX + sourceId + "]", e);
		}
	}
}
