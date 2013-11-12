package edu.mayo.mprc.daemon.monitor;

import java.io.Serializable;
import java.util.Date;

/**
 * Last status of a daemon.
 *
 * @author Roman Zenka
 */
public final class DaemonStatus implements Serializable {
	private static final long serialVersionUID = 9068736183236074388L;
	public static final long MILLIS_IN_SECOND = 1000L;

	private long lastResponse;
	private String message;
	private boolean ok;

	public DaemonStatus() {
		this(new Date(), "", true);
	}

	public DaemonStatus(final String message) {
		this(new Date(), message, false);
	}

	public DaemonStatus(final Date lastResponse, final String message, final boolean ok) {
		this.lastResponse = lastResponse.getTime();
		this.message = message;
		this.ok = ok;
	}

	/**
	 * @param timeoutSeconds How many seconds need to elapse for us to consider the daemon non-responding.
	 * @return True if this status is older than given amount of seconds.
	 */
	public boolean isTooOld(final long timeoutSeconds) {
		return new Date().getTime() - lastResponse > (long) timeoutSeconds * MILLIS_IN_SECOND;
	}

	public long getLastResponse() {
		return lastResponse;
	}

	public String getMessage() {
		return message;
	}

	public boolean isOk() {
		return ok;
	}
}
