package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.io.Serializable;

/**
 * A subset of the information a daemon has pertaining to file sharing.
 */
public final class DaemonConfigInfo implements Serializable {
	private static final long serialVersionUID = 20101119L;
	private String daemonId;
	private String sharedFileSpacePath;
	private String tempFolderPath;

	public DaemonConfigInfo() {
	}

	public DaemonConfigInfo(final String daemonId, final String sharedFileSpacePath, final String tempFolderPath) {
		this.daemonId = daemonId;
		setSharedFileSpacePath(sharedFileSpacePath);
		setTempFolderPath(tempFolderPath);
	}

	private static String toCanonical(final String path) {
		if (path != null) {
			if (!path.isEmpty()) {
				return FileUtilities.canonicalDirectoryPath(new File(path));
			} else {
				return "";
			}
		} else {
			throw new MprcException("The daemon cannot have file space or log paths set to null");
		}
	}

	public String getDaemonId() {
		return daemonId;
	}

	public void setDaemonId(final String daemonId) {
		this.daemonId = daemonId;
	}

	public String getSharedFileSpacePath() {
		if (sharedFileSpacePath != null && sharedFileSpacePath.isEmpty()) {
			sharedFileSpacePath = null;
		}
		return sharedFileSpacePath;
	}

	public void setSharedFileSpacePath(final String sharedFileSpacePath) {
		this.sharedFileSpacePath = toCanonical(sharedFileSpacePath);
	}

	public String getTempFolderPath() {
		if (tempFolderPath != null && tempFolderPath.isEmpty()) {
			tempFolderPath = null;
		}
		return tempFolderPath;
	}

	public void setTempFolderPath(final String tempFolderPath) {
		this.tempFolderPath = toCanonical(tempFolderPath);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof DaemonConfigInfo) {
			return daemonId.equals(((DaemonConfigInfo) obj).getDaemonId());
		}

		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		int result = daemonId != null ? daemonId.hashCode() : 0;

		return result;
	}

	@Override
	public String toString() {
		return "DaemonConfigInfo{" +
				"daemonId='" + daemonId + '\'' +
				", sharedFileSpacePath='" + sharedFileSpacePath + '\'' +
				", tempFolderPath='" + tempFolderPath + '\'' +
				'}';
	}
}
