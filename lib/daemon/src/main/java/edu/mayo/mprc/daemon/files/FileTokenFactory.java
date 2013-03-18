package edu.mayo.mprc.daemon.files;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.filesharing.FileTransfer;
import edu.mayo.mprc.filesharing.FileTransferHandler;
import edu.mayo.mprc.filesharing.jms.JmsFileTransferHandlerFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Key class for handling {@link FileToken} classes. A {@link FileToken} is basically path to a file + information about the daemon
 * where the file resides. These tokens are needed when sharing files over shared filesystem between daemons.
 * <p/>
 * <code>FileTokenFactory</code> not only creates {@link FileToken} classes, it also performs translations of the paths
 * contained in them. The usage is following:
 * <p/>
 * <ol>
 * <li>{@link #createAnonymousFileToken} - makes a new token that does not yet know about where it came from.
 * This is a static method you can call anytime.</li>
 * <li>Before the token gets sent over the wire, {@link #translateBeforeTransfer(FileToken)}
 * is called, which gives the tokens information about who is the actual sender.</li>
 * <li>When the token is received, {@link #getFile(FileToken)}
 * is called to turn it into a file.
 * <li>Because tokens can refer to objects that will be created in the future (e.g. "put my files here"), there is last
 * step - calling either {@link #uploadAndWait(FileToken)} or other {@link FileTokenSynchronizer} methods.
 * </li>
 * </ol>
 */
@Component("fileTokenFactory")
public final class FileTokenFactory implements SenderTokenTranslator, ReceiverTokenTranslator, FileTokenSynchronizer {

	private static final Logger LOGGER = Logger.getLogger(FileTokenFactory.class);

	private DaemonConfigInfo daemonConfigInfo;
	private DaemonConfigInfo databaseDaemonConfigInfo;

	//For sharing files that  can not be access through shared file system.
	private JmsFileTransferHandlerFactory fileSharingFactory;
	private FileTransferHandler fileTransferHandler;
	private File tempFolderRepository;
	private static final String DEFAULT_lOCAL_FILE_REPOSITORY_DIRECTORY = "localFileSharingRepository";

	public static final String SHARED_TYPE_PREFIX = "shared:";
	public static final String LOCAL_TYPE_PREFIX = "local:";
	public static final String FILE_TAG = "file";


	private static final ExecutorService fileTransferService;

	static {
		fileTransferService = Executors.newFixedThreadPool(4,
				new ThreadFactoryBuilder()
						.setDaemon(false)
						.setNameFormat("fileTransfer-%d")
						.build());
	}

	public FileTokenFactory() {
	}

	public FileTokenFactory(final DaemonConfigInfo daemonConfigInfo) {
		this.daemonConfigInfo = daemonConfigInfo;
	}

	public DaemonConfigInfo getDaemonConfigInfo() {
		return daemonConfigInfo;
	}

	public void setDaemonConfigInfo(final DaemonConfigInfo daemonConfigInfo) {
		this.daemonConfigInfo = daemonConfigInfo;
	}

	public DaemonConfigInfo getDatabaseDaemonConfigInfo() {
		return databaseDaemonConfigInfo;
	}

	public void setDatabaseDaemonConfigInfo(final DaemonConfigInfo databaseDaemonConfigInfo) {
		this.databaseDaemonConfigInfo = databaseDaemonConfigInfo;
	}

	public JmsFileTransferHandlerFactory getFileSharingFactory() {
		return fileSharingFactory;
	}

	/**
	 * Sets the FileSharingFactory object and starts processing FileToken transfer requests.
	 *
	 * @param fileSharingFactory
	 * @param processRemoteRequests true if FileToken transfer request from remote systems are to be processed. Otherwise, false.
	 */
	public void setFileSharingFactory(final JmsFileTransferHandlerFactory fileSharingFactory, final boolean processRemoteRequests) {
		this.fileSharingFactory = fileSharingFactory;
		startFileSharing(processRemoteRequests);
	}

	/**
	 * Sets the FileSharingFactory object and starts processing remote and local FileToken transfer requests.
	 *
	 * @param fileSharingFactory
	 */
	public void setFileSharingFactory(final JmsFileTransferHandlerFactory fileSharingFactory) {
		this.fileSharingFactory = fileSharingFactory;
		startFileSharing(true);
	}

	public File getTempFolderRepository() {
		if (tempFolderRepository == null) {
			tempFolderRepository = new File(FileUtilities.DEFAULT_TEMP_DIRECTORY, DEFAULT_lOCAL_FILE_REPOSITORY_DIRECTORY).getAbsoluteFile();
			FileUtilities.ensureFolderExists(tempFolderRepository);

			LOGGER.info("FileTokenFactory is using local file sharing repository [" + tempFolderRepository.getAbsolutePath() + "]");
		}
		return tempFolderRepository;
	}

	public void setTempFolderRepository(final File tempFolderRepository) {
		this.tempFolderRepository = tempFolderRepository;
		LOGGER.info("FileTokenFactory is using local file sharing repository [" + tempFolderRepository.getAbsolutePath() + "]");
	}

	/**
	 * Trnaslate file to token having this daemon as source.
	 *
	 * @param file
	 * @return
	 */
	public FileToken getFileToken(final File file) {
		return getFileTokenLocal(daemonConfigInfo, file);
	}

	/**
	 * Creates FileToken object without source DaemonConfigInfo object.
	 * This utility method can be used when a FileToken object is required and
	 * the source DaemonConfigInfo object is not known.
	 *
	 * @param file
	 * @return
	 */
	public static FileToken createAnonymousFileToken(final File file) {
		if (file == null) {
			return null;
		}
		try {
			return new SharedToken(null, FileUtilities.getCanonicalFileNoLinks(file).toURI().toString(), file.exists());
		} catch (Exception e) {
			throw new MprcException("Anonymous FileToken object could not be created.", e);
		}
	}

	/**
	 * We have an object that holds a file token that we are about to send.
	 * This token can be:
	 * <dl>
	 * <dt><c>null</c></dt>
	 * <dd>Return a null token.</dd>
	 * <dt>Anonymous</dt>
	 * <dd>Set the source daemon to be us</dd>
	 * <dt>From another daemon</dt>
	 * <dd>Keep the token as-is - we are only passing it along</dd>
	 * </dl>
	 *
	 * @param fileToken Token to be translated.
	 * @return Translated token - it's source daemon is properly filled in if it was not previously
	 */
	public FileToken translateBeforeTransfer(final FileToken fileToken) {
		if (fileToken == null) {
			return null;
		}
		if (fileToken.getSourceDaemonConfigInfo() == null) {
			// This is an anonymous token. Make it specific to our daemon.
			try {
				return getFileToken(new File(new URI(fileToken.getTokenPath())));
			} catch (URISyntaxException e) {
				throw new MprcException("FileToken object could not be regenerated.", e);
			}
		} else {
			return fileToken;
		}
	}

	@Override
	public FileToken translateBeforeTransfer(File file) {
		return translateBeforeTransfer(createAnonymousFileToken(file));
	}

	/**
	 * Translate FileToken to file in this daemon specific file system.
	 *
	 * @param fileToken
	 * @return
	 */
	public File getFile(final FileToken fileToken) {
		if (fileToken == null) {
			return null;
		}

		if (isFileTokenShared(fileToken)) {
			//Shared to shared
			return new File(daemonConfigInfo.getSharedFileSpacePath(), removePrefixFromToken(fileToken.getTokenPath(), SHARED_TYPE_PREFIX));
		} else if (isFileTokenLocal(fileToken)) {
			// Transfer within the same system, expect that the token is local:
			return new File(removePrefixFromToken(fileToken.getTokenPath(), LOCAL_TYPE_PREFIX));
		} else {
			//No shared between source and destination systems.
			try {
				final FileTransfer fileTransfer = getFileTransferHandler().getFile(fileToken.getSourceDaemonConfigInfo().getDaemonId(), getSpecificFilePathFromToken(fileToken), getLocalFileForRemoteToken(fileToken));
				return fileTransfer.done().get(0);

			} catch (Exception e) {
				throw new MprcException("Could not get file from file token. FileToken: " + fileToken.toString(), e);
			}
		}
	}

	public String fileToDatabaseToken(final File file) {
		if (file == null) {
			return null;
		}
		if (databaseDaemonConfigInfo != null) {
			final FileToken fileToken = getFileToken(file);
			return translateFileToken(fileToken, databaseDaemonConfigInfo).getTokenPath();
		} else {
			throw new MprcException("Database DaemonConfigInfo object is not set. Can not translate file to database String token.");
		}
	}

	public File databaseTokenToFile(final String tokenPath) {
		if (tokenPath == null) {
			return null;
		}
		if (databaseDaemonConfigInfo != null) {
			final FileToken fileToken = new SharedToken(databaseDaemonConfigInfo, tokenPath, false);
			return getFile(fileToken);
		} else {
			throw new MprcException("Database DaemonConfigInfo object is not set. Can not translate database String token to File.");
		}
	}

	public String fileToTaggedDatabaseToken(final File file) {
		return "<" + FILE_TAG + ">" + fileToDatabaseToken(file) + "</" + FILE_TAG + ">";
	}

	public static String tagDatabaseToken(final String databaseToken) {
		return "<" + FILE_TAG + ">" + databaseToken + "</" + FILE_TAG + ">";
	}

	/**
	 * Given a remote daemon FileToken, this method generates the corresponding local FileToken.
	 * If the remote daemon and this daemon do not have common shared file space, local File
	 * is created in a local folder that is determined by the value of the given LocalFileType.
	 *
	 * @param fileToken
	 * @return
	 */
	private File getLocalFileForRemoteToken(final FileToken fileToken) {
		final File rootFolder = new File(getTempFolderRepository(), fileToken.getSourceDaemonConfigInfo().getDaemonId());

		FileUtilities.ensureFolderExists(rootFolder);

		if (fileTokenOnSharedPath(fileToken)) {
			return new File(rootFolder, removePrefixFromToken(fileToken.getTokenPath(), SHARED_TYPE_PREFIX));
		} else {
			String relativePath = null;

			if (fileToken.getTokenPath().startsWith(LOCAL_TYPE_PREFIX)) {
				relativePath = removePrefixFromToken(fileToken.getTokenPath(), LOCAL_TYPE_PREFIX);
			} else {
				relativePath = fileToken.getTokenPath();
			}

			//Todo: Review logic to remove root of windows path. This logic handles c: root, but //rome/mprc may failed.
			final int index = relativePath.indexOf(':');
			if (index != -1) {
				relativePath = relativePath.substring(index + 1);
			}

			return new File(rootFolder, relativePath);
		}
	}

	/**
	 * Returns a file path built from this file token and relative to its source daemon.
	 *
	 * @param fileToken
	 * @return
	 */
	private String getSpecificFilePathFromToken(final FileToken fileToken) {
		if (fileTokenOnSharedPath(fileToken)) {
			String sharedPath = fileToken.getSourceDaemonConfigInfo().getSharedFileSpacePath();

			if (sharedPath.endsWith("/")) {
				sharedPath = sharedPath.substring(0, sharedPath.length() - 1);
			}

			return sharedPath + removePrefixFromToken(fileToken.getTokenPath(), SHARED_TYPE_PREFIX);
		} else {
			if (fileToken.getTokenPath().startsWith(LOCAL_TYPE_PREFIX)) {
				return removePrefixFromToken(fileToken.getTokenPath(), LOCAL_TYPE_PREFIX);
			}

			return fileToken.getTokenPath();
		}
	}

	private synchronized void startFileSharing(final boolean processRemoteRequests) {
		if (fileSharingFactory != null) {
			// The fileSharingFactory can be null, that is valid for DatabaseValidator
			if (fileTransferHandler == null) {
				fileTransferHandler = fileSharingFactory.createFileSharing(daemonConfigInfo.getDaemonId());
			}
		}

		fileTransferService.submit(new Thread("Starting FileTransferHandler") {
			public void run() {
				synchronized (FileTokenFactory.this) {
					fileTransferHandler.startProcessingRequests(processRemoteRequests);
				}
			}
		});
	}

	private synchronized FileTransferHandler getFileTransferHandler() {
		return fileTransferHandler;
	}

	private FileToken getFileToken(final String fileAbsolutePath) {
		return getFileToken(new File(fileAbsolutePath));
	}

	private String addPrefixToPath(final String prefix, final String path) {
		if (path.startsWith("/")) {
			return prefix + path;
		}
		return prefix + "/" + path;
	}

	private FileToken getFileTokenLocal(final DaemonConfigInfo sourceDaemonConfigInfo, final File file) {
		final String filePath = canonicalFilePath(file);

		if (sourceDaemonConfigInfo.getSharedFileSpacePath() == null) {
			return new SharedToken(sourceDaemonConfigInfo, addPrefixToPath(LOCAL_TYPE_PREFIX, filePath), file.exists());
		} else {
			if (filePath.length() < sourceDaemonConfigInfo.getSharedFileSpacePath().length()) {
				return fileToLocalToken(sourceDaemonConfigInfo, file);
			}
			final String filePathPrefix = filePath.substring(0, sourceDaemonConfigInfo.getSharedFileSpacePath().length());

			if (filePathPrefix.equals(sourceDaemonConfigInfo.getSharedFileSpacePath()) && filePathPrefix.length() > 0) {
				return new SharedToken(sourceDaemonConfigInfo, addPrefixToPath(SHARED_TYPE_PREFIX, filePath.substring(sourceDaemonConfigInfo.getSharedFileSpacePath().length())), file.exists());
			} else {
				return fileToLocalToken(sourceDaemonConfigInfo, file);
			}
		}
	}

	private FileToken fileToLocalToken(final DaemonConfigInfo sourceDaemonConfigInfo, final File file) {
		if (daemonConfigInfo.equals(sourceDaemonConfigInfo)) {
			return new SharedToken(sourceDaemonConfigInfo, addPrefixToPath(LOCAL_TYPE_PREFIX, canonicalFilePath(file)), file.exists());
		} else {
			return throwLocalUnsupported(file);
		}
	}

	private static FileToken throwLocalUnsupported(final File file) {
		throw new MprcException("Transfer of nonshared files between systems is not supported yet - trying to transfer " + file.getPath());
	}

	private FileToken translateFileToken(final FileToken fileToken, final DaemonConfigInfo destinationDaemonConfigInfo) {
		if (fileToken.getSourceDaemonConfigInfo() == null) {
			return translateFileToken(getFileToken(fileToken.getTokenPath()), destinationDaemonConfigInfo);
		} else if (fileToken.getSourceDaemonConfigInfo().getSharedFileSpacePath() != null && destinationDaemonConfigInfo.getSharedFileSpacePath() != null && fileTokenOnSharedPath(fileToken)) {
			return new SharedToken(destinationDaemonConfigInfo, fileToken.getTokenPath(), fileToken.existsOnSourceDaemon());
		} else if (fileToken.getSourceDaemonConfigInfo().equals(destinationDaemonConfigInfo)) {
			// Transfer within the same daemon
			return new SharedToken(destinationDaemonConfigInfo, fileToken.getTokenPath(), fileToken.existsOnSourceDaemon());
		} else {
			return translateFileToken(getFileToken(getFile(fileToken)), destinationDaemonConfigInfo);
		}
	}

	private static boolean fileTokenOnSharedPath(final FileToken fileToken) {
		return fileToken.getTokenPath().startsWith(SHARED_TYPE_PREFIX);
	}

	private static String removePrefixFromToken(final String token, final String prefix) {
		if (!token.startsWith(prefix)) {
			throw new MprcException("The given token '" + token + "' does not start with " + prefix + "");
		}
		return token.substring(prefix.length());
	}

	/**
	 * Returns canonical path to a given file. The canonicality means:
	 * <ul>
	 * <li>uses only forward slashes</li>
	 * <li>lowercase/uppercase, links, ., .. are resolved via {@link edu.mayo.mprc.utilities.FileUtilities#getCanonicalFileNoLinks}.</li>
	 * </ul>
	 * <p/>
	 * / is appended in case the file refers to existing directory.
	 *
	 * @param file File to get path of.
	 * @return Canonical file path.
	 */
	public static String canonicalFilePath(final File file) {
		String path;
		try {
			path = FileUtilities.removeFileUrlPrefix(FileUtilities.getCanonicalFileNoLinks(file).toURI());
		} catch (Exception ignore) {
			path = FileUtilities.removeFileUrlPrefix(file.getAbsoluteFile().toURI());
		}
		return path;
	}

	@Override
	public void upload(final FileToken fileToken) {
		fileTransferService.submit(new Thread("Token synchronization: " + fileToken.getTokenPath()) {
			public void run() {
				uploadAndWait(fileToken);
			}
		});
	}

	@Override
	public void uploadAndWait(final FileToken fileToken) {
		final FileTransfer fileTransfer = uploadFile(fileToken);

		if (fileTransfer != null) {
			fileTransfer.done();

			if (fileTransfer.getErrorException() != null) {
				throw new MprcException("File synchronization failed. FileToken: " + fileToken.toString(), fileTransfer.getErrorException());
			}
		}
	}

	public void download(final FileToken theirToken) {
		fileTransferService.submit(new Thread("Token synchronization: " + theirToken.getTokenPath()) {
			public void run() {
				downloadAndWait(theirToken);
			}
		});
	}

	public void downloadAndWait(final FileToken theirToken) {
		final FileTransfer fileTransfer = downloadFile(theirToken);

		if (fileTransfer != null) {
			fileTransfer.done();

			if (fileTransfer.getErrorException() != null) {
				throw new MprcException("File synchronization failed. FileToken: " + theirToken.toString(), fileTransfer.getErrorException());
			}
		}
	}

	/**
	 * Synchronizes file token in its source system with corresponding local file token.
	 *
	 * @param fileToken
	 * @return
	 */
	private FileTransfer uploadFile(final FileToken fileToken) {
		//Synchronized FileToken if the FileToken is not in shared file space.
		if (!isFileTokenShared(fileToken) && !isFileTokenLocal(fileToken)) {
			final String destinationId = fileToken.getSourceDaemonConfigInfo().getDaemonId();
			final File localFile = getLocalFileForRemoteToken(fileToken);
			final String remoteFilePath = getSpecificFilePathFromToken(fileToken);

			try {
				if (!localFile.isDirectory()) {
					return getFileTransferHandler().uploadFile(destinationId, localFile, remoteFilePath);
				} else {
					return getFileTransferHandler().uploadFolder(destinationId, localFile, remoteFilePath);
				}
			} catch (Exception e) {
				throw new MprcException("FileToken could not be synchronized. Remote system [" + destinationId + "], Remote file [" + remoteFilePath + "]", e);
			}
		}

		return null;
	}

	private FileTransfer downloadFile(final FileToken fileToken) {
		//Synchronized FileToken if the FileToken is not in shared file space.
		if (!isFileTokenShared(fileToken) && !isFileTokenLocal(fileToken)) {
			final String destinationId = fileToken.getSourceDaemonConfigInfo().getDaemonId();
			final File localFile = getLocalFileForRemoteToken(fileToken);
			final String remoteFilePath = getSpecificFilePathFromToken(fileToken);

			try {
				return getFileTransferHandler().downloadFile(destinationId, localFile, remoteFilePath);
			} catch (Exception e) {
				throw new MprcException("FileToken could not be synchronized. Remote system [" + destinationId + "], Remote file [" + remoteFilePath + "]", e);
			}
		}

		return null;
	}

	private boolean isFileTokenShared(final FileToken fileToken) {
		return daemonConfigInfo.getSharedFileSpacePath() != null && fileToken.getSourceDaemonConfigInfo().getSharedFileSpacePath() != null && fileTokenOnSharedPath(fileToken);
	}

	private boolean isFileTokenLocal(final FileToken fileToken) {
		return daemonConfigInfo.equals(fileToken.getSourceDaemonConfigInfo());
	}

	private static final class SharedToken implements FileToken {
		private static final long serialVersionUID = 20111119L;
		private DaemonConfigInfo sourceDaemonConfigInfo;
		private String tokenPath;
		private boolean existsOnSource;

		private SharedToken(final DaemonConfigInfo sourceDaemonConfigInfo, final String tokenPath, boolean existsOnSource) {
			this.sourceDaemonConfigInfo = sourceDaemonConfigInfo;
			this.tokenPath = tokenPath;
			this.existsOnSource = existsOnSource;
		}

		public String getTokenPath() {
			return tokenPath;
		}

		@Override
		public boolean existsOnSourceDaemon() {
			return existsOnSource;
		}

		public DaemonConfigInfo getSourceDaemonConfigInfo() {
			return sourceDaemonConfigInfo;
		}

		@Override
		public String toString() {
			if (sourceDaemonConfigInfo != null) {
				return "Daemon id: " + sourceDaemonConfigInfo.getDaemonId() +
						", Shared path: " + sourceDaemonConfigInfo.getSharedFileSpacePath() + ", Token path: " + tokenPath;
			} else {
				return "No daemon assigned, Token path: " + tokenPath;
			}
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof SharedToken) {
				final SharedToken sharedToken = (SharedToken) obj;

				return sharedToken.getTokenPath().equals(tokenPath) && sharedToken.getSourceDaemonConfigInfo().equals(sourceDaemonConfigInfo);
			}

			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			int result = sourceDaemonConfigInfo != null ? sourceDaemonConfigInfo.hashCode() : 0;
			result = 31 * result + (tokenPath != null ? tokenPath.hashCode() : 0);
			return result;
		}
	}
}
