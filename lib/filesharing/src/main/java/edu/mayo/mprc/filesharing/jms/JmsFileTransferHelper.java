package edu.mayo.mprc.filesharing.jms;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.filesharing.FileTransfer;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper class. Actually opens sockets to transfer files around.
 */
public final class JmsFileTransferHelper {
	private static AtomicLong lastRequestId = new AtomicLong(System.currentTimeMillis());

	private static final Logger LOGGER = Logger.getLogger(JmsFileTransferHelper.class);

	public static final String FILE_TRANSFER_MAX_THREAD = "edu.mayo.mprc.filesharing.jms.JmsFileTransferHelper.FileTransferMaxThread";
	public static final String FILE_TRANSFER_MAX_THREAD_DEFAULT = "10";

	/**
	 * We want to control the number of threads that are spun to process file transfers.
	 */
	private static final ExecutorService fileTransferThreadExecutorService;

	private JmsFileTransferHelper() {
	}

	static {
		int fileTransferMaxThread = Integer.parseInt(System.getProperty(FILE_TRANSFER_MAX_THREAD, FILE_TRANSFER_MAX_THREAD_DEFAULT));

		if (fileTransferMaxThread <= 2) {
			fileTransferMaxThread = Integer.parseInt(FILE_TRANSFER_MAX_THREAD_DEFAULT);
		}

		fileTransferThreadExecutorService = Executors.newFixedThreadPool(fileTransferMaxThread);
	}

	/**
	 * Handles multi file transfer from local system to remote system.
	 *
	 * @param session  JMS Session
	 * @param producer JMS producer for remote system.
	 * @param fromTo   Pairs of corresponding local files and remote files.
	 * @return
	 */
	public static FileTransfer upload(final Session session, final MessageProducer producer, final Map<File, String> fromTo) {
		return transferFiles(session, producer, fromTo, false);
	}

	/**
	 * Handles single file transfer from remote system to this system.
	 *
	 * @param session  JMS Session
	 * @param producer JMS producer for remote system.
	 * @param from
	 * @param to
	 * @return
	 */
	public static FileTransfer download(final Session session, final MessageProducer producer, final String from, final File to) {

		final Map<File, String> toFrom = new TreeMap<File, String>();
		toFrom.put(to, from);

		return download(session, producer, toFrom);
	}

	/**
	 * Handles multi file transfer from remote system to local system.
	 *
	 * @param session  JMS Session
	 * @param producer JMS producer for remote system.
	 * @param toFrom   Pairs of corresponding local files and remote files.
	 * @return
	 */
	public static FileTransfer download(final Session session, final MessageProducer producer, final Map<File, String> toFrom) {
		return transferFiles(session, producer, toFrom, true);
	}

	private static FileTransfer transferFiles(final Session session, final MessageProducer producer, final Map<File, String> localRemote, final boolean remoteToLocal) {
		//Create reversed map of file paths and files. This will be used ahead in the code.
		final Map<String, File> remoteFilePathLocalFilePairs = reverseMap(localRemote);

		final ServerSocket serverSocket;
		MultiFileTransferRequest fileTransferRequest = null;

		if (remoteToLocal) {
			try {
				serverSocket = new ServerSocket(0);
			} catch (IOException e) {
				throw new MprcException("Could not open a server socket on any free port", e);
			}
			fileTransferRequest = new MultiFileTransferRequest(lastRequestId.incrementAndGet(), getFileInfos(localRemote), new InetSocketAddress(getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
		} else {
			serverSocket = null;
			fileTransferRequest = new MultiFileTransferRequest(lastRequestId.incrementAndGet(), getFileInfos(localRemote));
			fileTransferRequest.setBeSource(true);
		}

		final ObjectMessage objectMessage;
		try {
			objectMessage = session.createObjectMessage(fileTransferRequest);
		} catch (JMSException e) {
			throw new MprcException("Could not create a JMS message", e);
		}
		try {
			objectMessage.setJMSReplyTo(session.createTemporaryQueue());
		} catch (JMSException e) {
			throw new MprcException("Could not create a temporary queue", e);
		}

		final FileTransfer fileTransfer = new FileTransfer(new LinkedList<File>(localRemote.keySet()));
		final List<File> transferedFiles = Collections.synchronizedList(new ArrayList<File>(localRemote.size()));
		fileTransfer.setTransferedFiles(transferedFiles);

		final AtomicInteger transferedFileCounter = new AtomicInteger(0);

		//Expect reply with list of existing remote files.
		final Destination jmsReplyTo;
		try {
			jmsReplyTo = objectMessage.getJMSReplyTo();
		} catch (JMSException e) {
			throw new MprcException("Could not determine the reply to field on the message", e);
		}
		final MessageConsumer consumer;
		try {
			consumer = session.createConsumer(jmsReplyTo);
		} catch (JMSException e) {
			throw new MprcException("Could not create JMS message consumer", e);
		}
		try {
			consumer.setMessageListener(new MessageListener() {
				public void onMessage(final Message message) {
					try {
						LOGGER.debug("Acknowledge message: " + message.toString());

						if (!(message instanceof ObjectMessage)) {
							ExceptionUtilities.throwCastException(message, ObjectMessage.class);
							return;
						}
						final Object object = ((ObjectMessage) message).getObject();

						if (object instanceof MultiFileTransferResponse) {
							final MultiFileTransferResponse multiFileTransferResponse = (MultiFileTransferResponse) object;

							LOGGER.debug("Received file transefer response: " + multiFileTransferResponse.getRequestId());

							transferedFileCounter.addAndGet(multiFileTransferResponse.getFileInfos().size());
							FileTransferThread fileTransferThread = null;

							try {
								LOGGER.debug("Client side request: " + multiFileTransferResponse.getRequestId() + ". Transfering " + multiFileTransferResponse.getFileInfos().size() + " file(s).");

								for (final FileInfo fileInfo : multiFileTransferResponse.getFileInfos()) {
									final File file = remoteFilePathLocalFilePairs.get(fileInfo.getFilePath());

									LOGGER.debug("Creating client side socket for request: " + multiFileTransferResponse.getRequestId() + " and file: " + fileInfo.toString());

									if (remoteToLocal) {
										LOGGER.debug("Client side request: " + multiFileTransferResponse.getRequestId() + ". Listening to server socket port: " + serverSocket.getLocalPort());
										fileTransferThread = new SocketToFileTransferThread(new FileInfo(file.getAbsolutePath(), fileInfo.getLength(), fileInfo.getLastModified()), serverSocket.accept());
									} else {
										//If the source files are the local files, initiate the file transfers.
										LOGGER.debug("Client side request: " + multiFileTransferResponse.getRequestId() + ". Creating a socket at: " + multiFileTransferResponse.getInetSocketAddress().getPort());
										fileTransferThread = new FileToSocketTransferThread(file, new Socket(multiFileTransferResponse.getInetSocketAddress().getAddress(), multiFileTransferResponse.getInetSocketAddress().getPort()));
									}

									fileTransferThread.setTransferCompleteListener(new TransferCompleteListener() {
										@Override
										public void transferCompleted(final TransferCompleteEvent event) {
											LOGGER.info("Client side request: " + multiFileTransferResponse.getRequestId() + ". Completed transfering file [" + file.getAbsolutePath() + "]");

											if (event.getException() != null && fileTransfer.getErrorException() == null) {
												fileTransfer.setErrorException(event.getException());
											} else if (event.getException() == null) {
												transferedFiles.add(file);
											}

											synchronized (transferedFileCounter) {
												if (transferedFileCounter.decrementAndGet() == 0) {
													//Notify any thread waiting on this counter to be zero.
													transferedFileCounter.notifyAll();
												}
											}
										}
									});

									LOGGER.info("Client side request: " + multiFileTransferResponse.getRequestId() + ". Starting transfering file [" + file.getAbsolutePath() + "]");

									fileTransferThreadExecutorService.submit(fileTransferThread);
								}
							} finally {
								if (remoteToLocal) {
									LOGGER.debug("Client side request: " + multiFileTransferResponse.getRequestId() + ". Closing server socket at: " + serverSocket.getLocalPort());
								}
								FileUtilities.closeObjectQuietly(serverSocket);
							}

							//Propagate deletions
							if (remoteToLocal) {
								for (final FileInfo fileInfo : multiFileTransferResponse.getNotExistingFileInfos()) {
									FileUtilities.deleteNow(remoteFilePathLocalFilePairs.get(fileInfo.getFilePath()));
								}
							}

						} else if (object instanceof RemoteTransferCompleteEvent) {
							final RemoteTransferCompleteEvent remoteTransferCompleteEvent = (RemoteTransferCompleteEvent) object;

							LOGGER.debug("Received file transfer complete event: " + remoteTransferCompleteEvent.getRequestId());

							if (remoteTransferCompleteEvent.getException() != null && fileTransfer.getErrorException() == null) {
								fileTransfer.setErrorException(remoteTransferCompleteEvent.getException());
							}

							if (remoteTransferCompleteEvent.getException() == null) {
								synchronized (transferedFileCounter) {
									if (transferedFileCounter.get() != 0) {
										// TODO: Ok, so if not all file got transfered, we wait for a minute (?) and then keep going???
										transferedFileCounter.wait(60000);
									}
								}
							}

							LOGGER.info("Client side request: " + remoteTransferCompleteEvent.getRequestId() + ". Transfered " + fileTransfer.getTransferedFiles().size() + " files of " + localRemote.size() + " requested");

							fileTransfer.setDone();
						} else {
							fileTransfer.setErrorException(new MprcException("ObjectMessage object must be of the type " + MultiFileTransferResponse.class.getName() + " or " + RemoteTransferCompleteEvent.class.getName() + "."));
							fileTransfer.setDone();
						}
					} catch (Exception e) {
						if (fileTransfer.getErrorException() != null) {
							fileTransfer.setErrorException(new MprcException("File(s) transfer could not be completed.", e));
						}

						fileTransfer.setDone();
					}
				}
			});
		} catch (JMSException e) {
			throw new MprcException("Could not set JMS message listener", e);
		}

		LOGGER.debug("Sending file transefer request: " + fileTransferRequest.getRequestId() + " with " + fileTransferRequest.getFileInfos().size() + " file(s).");
		try {
			producer.send(objectMessage);
		} catch (JMSException e) {
			throw new MprcException("Could not send a JMS message", e);
		}

		return fileTransfer;
	}

	private static InetAddress getLocalHost() {
		InetAddress localHost;
		try {
			localHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new MprcException("Could not determine localhost address", e);
		}
		return localHost;
	}

	public static void processMultiFileTransferRequest(final MultiFileTransferRequest multiFileTransferRequest, final Session session, final Destination requester) {

		LOGGER.debug("Received file transefer request: " + multiFileTransferRequest.getRequestId());

		final ServerSocket serverSocket;

		//If the request is to transfer local files, send requester the list of local valid files.
		final List<FileInfo> modifiedFileInfos = new ArrayList<FileInfo>();
		final List<FileInfo> notExistingFileInfos = new ArrayList<FileInfo>();
		getModifiedFileInfos(multiFileTransferRequest.getFileInfos(), modifiedFileInfos, notExistingFileInfos, multiFileTransferRequest.areSource());

		final RemoteTransferCompleteEvent remoteTransferCompleteEvent = new RemoteTransferCompleteEvent(multiFileTransferRequest.getRequestId(), modifiedFileInfos);
		final AtomicInteger transferedFileCounter = new AtomicInteger(modifiedFileInfos.size());

		MultiFileTransferResponse multiFileTransferResponse = null;
		Exception processingException = null;

		if (!multiFileTransferRequest.areSource()) {
			//If this system is the source, transfer must be initiated from system requesting transfer.
			serverSocket = null;
			multiFileTransferResponse = new MultiFileTransferResponse(multiFileTransferRequest.getRequestId(), modifiedFileInfos);
			multiFileTransferResponse.setNotExistingFileInfos(notExistingFileInfos);
		} else {
			try {
				serverSocket = new ServerSocket(0);
			} catch (IOException e) {
				throw new MprcException("Could not create server socket on a free port", e);
			}
			multiFileTransferResponse = new MultiFileTransferResponse(multiFileTransferRequest.getRequestId(), modifiedFileInfos, new InetSocketAddress(getLocalHost().getHostAddress(), serverSocket.getLocalPort()));
		}

		LOGGER.debug("Sending file transefer response: " + multiFileTransferResponse.getRequestId() + " with " + multiFileTransferResponse.getFileInfos().size() + " file(s).");

		try {
			final MessageProducer producer = session.createProducer(requester);
			final ObjectMessage objectMessage = session.createObjectMessage(multiFileTransferResponse);
			producer.send(objectMessage);
		} catch (JMSException e) {
			throw new MprcException("Could not send a JMS message", e);
		}

		try {
			FileTransferThread fileTransferThread = null;

			LOGGER.debug("Server side request: " + multiFileTransferResponse.getRequestId() + ". Transfering " + modifiedFileInfos.size() + " file(s).");

			for (final FileInfo fileInfo : modifiedFileInfos) {

				LOGGER.debug("Creating server side socket for request: " + multiFileTransferResponse.getRequestId() + " and file: " + fileInfo.toString());

				if (!multiFileTransferRequest.areSource()) {
					LOGGER.debug("Server side request: " + multiFileTransferResponse.getRequestId() + ". Creating a socket at: " + multiFileTransferRequest.getInetSocketAddress().getPort());
					fileTransferThread = new FileToSocketTransferThread(new File(fileInfo.getFilePath()), new Socket(multiFileTransferRequest.getInetSocketAddress().getAddress(), multiFileTransferRequest.getInetSocketAddress().getPort()));
				} else {
					LOGGER.debug("Server side request: " + multiFileTransferResponse.getRequestId() + ". Listening to server socket port: " + serverSocket.getLocalPort());
					fileTransferThread = new SocketToFileTransferThread(fileInfo, serverSocket.accept());
				}

				fileTransferThread.setTransferCompleteListener(new TransferCompleteListener() {
					@Override
					public void transferCompleted(final TransferCompleteEvent event) {
						LOGGER.info("Server side request: " + multiFileTransferRequest.getRequestId() + ". Completed transfering file [" + fileInfo.getFilePath() + "]");

						if (event.getException() != null && remoteTransferCompleteEvent.getException() == null) {
							remoteTransferCompleteEvent.setException(event.getException());
						}

						if (transferedFileCounter.decrementAndGet() == 0) {
							sendRemoteTransferCompleteEvent(remoteTransferCompleteEvent, session, requester);
						}
					}
				});

				LOGGER.info("Server side request: " + multiFileTransferResponse.getRequestId() + ". Starting transfering file [" + fileInfo.getFilePath() + "]");

				fileTransferThreadExecutorService.submit(fileTransferThread);
			}
		} catch (Exception e) {
			processingException = e;
			LOGGER.error("Error starting while starting transfer file threads.", e);
		} finally {
			if (multiFileTransferRequest.areSource()) {
				LOGGER.debug("Server side request: " + multiFileTransferResponse.getRequestId() + ". Closing server socket at: " + serverSocket.getLocalPort());
			}
			FileUtilities.closeObjectQuietly(serverSocket);
		}

		//If there is a failure to start processing files or no file needs transfer, send completion event.
		if (modifiedFileInfos.size() == 0 || processingException != null) {
			if (processingException != null && remoteTransferCompleteEvent.getException() == null) {
				remoteTransferCompleteEvent.setException(processingException);
			}

			sendRemoteTransferCompleteEvent(remoteTransferCompleteEvent, session, requester);
		}
	}

	private static void sendRemoteTransferCompleteEvent(final RemoteTransferCompleteEvent remoteTransferCompleteEvent, final Session session, final Destination requester) {
		try {
			LOGGER.debug("Sending file transefer complete event: " + remoteTransferCompleteEvent.getRequestId());
			session.createProducer(requester).send(session.createObjectMessage(remoteTransferCompleteEvent));
		} catch (JMSException e) {
			LOGGER.error("Could not send remote file transfer completion notification to requester.", e);
		}
	}

	private static List<FileInfo> getFileInfos(final Map<File, String> localFileRemoteFilePathPairs) {
		final List<FileInfo> fileInfos = new ArrayList(localFileRemoteFilePathPairs.size());

		for (final Map.Entry<File, String> mapEntry : localFileRemoteFilePathPairs.entrySet()) {
			if (!mapEntry.getKey().isDirectory() && mapEntry.getKey().exists()) {
				fileInfos.add(new FileInfo(mapEntry.getValue(), mapEntry.getKey().length(), mapEntry.getKey().lastModified()));
			} else {
				fileInfos.add(new FileInfo(mapEntry.getValue()));
			}
		}

		return fileInfos;
	}

	private static Map<String, File> reverseMap(final Map<File, String> localFileRemoteFilePathPairs) {
		final Map<String, File> remoteFilePathLocalFilePairs = new TreeMap<String, File>();

		for (final Map.Entry<File, String> me : localFileRemoteFilePathPairs.entrySet()) {
			remoteFilePathLocalFilePairs.put(me.getValue(), me.getKey());
		}

		return remoteFilePathLocalFilePairs;
	}

	private static void getModifiedFileInfos(final List<FileInfo> originalFileInfos, final List<FileInfo> modifiedFileInfos, final List<FileInfo> notExistingFileInfos, final boolean areSources) {
		File file = null;

		for (final FileInfo fileInfo : originalFileInfos) {
			file = new File(fileInfo.getFilePath());

			//This is a hack. If file is in shared file system, file may appear not to exist when it really does.
			FileUtilities.refreshFolder(file.getParentFile());

			if (file.exists() && !file.isDirectory() && file.length() > 0) {
				if ((fileInfo.getLastModified() == 0 && fileInfo.getLength() == 0) || (file.length() != fileInfo.getLength() || file.lastModified() != fileInfo.getLastModified())) {
					if (!areSources) {
						modifiedFileInfos.add(new FileInfo(fileInfo.getFilePath(), file.length(), file.lastModified()));
					} else {
						modifiedFileInfos.add(fileInfo);
					}
				}
			} else if (!file.exists() && fileInfo.getLength() > 0 && areSources) {
				modifiedFileInfos.add(fileInfo);
			} else if (!file.exists() && !areSources) {
				notExistingFileInfos.add(fileInfo);
			}
		}
	}
}
