package edu.mayo.mprc.daemon.files;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Key class for handling {@link FileToken} classes. A {@link FileToken} is basically path to a file + information about the daemon
 * where the file resides. These tokens are needed when sharing files over shared filesystem between daemons.
 * <p/>
 * {@code FileTokenFactory} not only creates {@link FileToken} classes, it also performs translations of the paths
 * contained in them. The usage is following:
 * <p/>
 * <ol>
 * <li>{@link #createAnonymousFileToken} - makes a new token that does not yet know about where it came from.
 * This is a static method you can call anytime.</li>
 * <li>Before the token gets sent over the wire, {@link #translateBeforeTransfer(FileToken)}
 * is called, which gives the tokens information about who is the actual sender.</li>
 * <li>When the token is received, {@link #getFile(FileToken)}
 * is called to turn it into a file.
 * </li>
 * </ol>
 */
@Component("fileTokenFactory")
public final class FileTokenFactory implements SenderTokenTranslator, ReceiverTokenTranslator {
	private DaemonConfigInfo daemonConfigInfo;
	private DaemonConfigInfo databaseDaemonConfigInfo;

	public static final String SHARED_TYPE_PREFIX = "shared:";
	public static final String LOCAL_TYPE_PREFIX = "local:";
	public static final String FILE_TAG = "file";

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
	@Override
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
	@Override
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
			throw new MprcException("Cannot transfer file between systems that do not share disk space. FileToken: " + fileToken.toString());
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

			if (filePathPrefix.equals(sourceDaemonConfigInfo.getSharedFileSpacePath()) && !filePathPrefix.isEmpty()) {
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

		@Override
		public String getTokenPath() {
			return tokenPath;
		}

		@Override
		public boolean existsOnSourceDaemon() {
			return existsOnSource;
		}

		@Override
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
