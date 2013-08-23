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

	public DaemonConfigInfo() {
	}

	public DaemonConfigInfo(final String daemonId, final String sharedFileSpacePath) {
		this.daemonId = daemonId;
		storeCanonical(sharedFileSpacePath);
	}

	private void storeCanonical(final String sharedFileSpacePath) {
		if (sharedFileSpacePath != null) {
			if (!sharedFileSpacePath.isEmpty()) {
				this.sharedFileSpacePath = FileUtilities.canonicalDirectoryPath(new File(sharedFileSpacePath));
			} else {
				this.sharedFileSpacePath = "";
			}
		} else {
			throw new MprcException("The daemon cannot have its shared file space path set to null");
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
		storeCanonical(sharedFileSpacePath);
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
		result = 31 * result + (sharedFileSpacePath != null ? sharedFileSpacePath.hashCode() : 0);
		return result;
	}
}
