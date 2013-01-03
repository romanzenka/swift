package edu.mayo.mprc.daemon.files;

import edu.mayo.mprc.config.DaemonConfigInfo;

import java.io.Serializable;

/**
 * A file token is a representation of a file as it exists on a particular computer.
 * A token knows three things:
 * <ul>
 * <li>source daemon (where it originated)</li>
 * <li>path to file on the source daemon</li>
 * <li>whether the file actually existed on the source daemon</li>
 * </ul>
 * <p/>
 * The receiver of a token can use the origin information to contact the proper daemon and ask for file contents.
 * The receiver also knows the best method of file transfer (knowing source and destination daemon configurations).
 * It can either use shared disk space (if available), or actually download the file to a temporary location.
 * <p/>
 * The main class that can create and convert {@link FileToken} objects is {@link FileTokenFactory}.
 */
public interface FileToken extends Serializable {

	/**
	 * Returns the DaemonConfigInfo of the daemon that created this file token.
	 *
	 * @return
	 */
	DaemonConfigInfo getSourceDaemonConfigInfo();

	/**
	 * Returns Token path identifying this file token, for example, the token path for a file
	 * object represented by a file token can be the absolute path of the file.
	 *
	 * @return
	 */
	String getTokenPath();

	/**
	 * Return true if the file exists on the daemon that crated this file token.
	 * This is used on NFS filesystem to ensure that if the file existed on source,
	 * it also exists on the destination as expected.
	 */
	boolean existsOnSourceDaemon();
}
