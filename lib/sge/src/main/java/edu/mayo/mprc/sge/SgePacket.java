package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.messaging.SerializedRequest;

import java.io.File;

/**
 * Class use to send work packet to grid engine. The object contains the actual work itself as {@link #getWorkPacket()},
 * but also contains information about how to send progress back from the Sun Grid Engine.
 */
public final class SgePacket extends FileHolder {

	private SerializedRequest serializedRequest;
	// Id of the worker to invoke (has to match Swift's configuration)
	private String serviceName;
	// For initializing FileTokenFactory - config of the daemon on whose behalf we run
	private DaemonConfigInfo daemonConfigInfo;

	// Where should we put extra logs (comes from the service/daemon configuration)
	private File logFolder;

	public SgePacket() {
	}

	public SgePacket(final SerializedRequest serializedRequest, final String serviceName, final DaemonConfigInfo daemonConfigInfo,
	                 final File logFolder) {
		setSerializedRequest(serializedRequest);
		setServiceName(serviceName);
		setDaemonConfigInfo(daemonConfigInfo);
		setLogFolder(logFolder);
	}

	public Object getWorkPacket() {
		return serializedRequest.getMessage();
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public SerializedRequest getSerializedRequest() {
		return serializedRequest;
	}

	public void setSerializedRequest(final SerializedRequest serializedRequest) {
		this.serializedRequest = serializedRequest;
	}

	public DaemonConfigInfo getDaemonConfigInfo() {
		return daemonConfigInfo;
	}

	public void setDaemonConfigInfo(final DaemonConfigInfo daemonConfigInfo) {
		this.daemonConfigInfo = daemonConfigInfo;
	}

	public File getLogFolder() {
		return logFolder;
	}

	public void setLogFolder(final File logFolder) {
		this.logFolder = logFolder;
	}
}
