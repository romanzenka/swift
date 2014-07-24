package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.messaging.SerializedRequest;

import java.io.File;

/**
 * Class use to send work packet to grid engine. The object contains the actual work itself as {@link #getWorkPacket()},
 * but also contains information about how to send progress back from the Sun Grid Engine.
 */
public final class SgePacket extends FileHolder {

	private SerializedRequest serializedRequest;
	// How to create the worker (assumes worker has no dependencies!)
	private ResourceConfig workerFactoryConfig;
	// For initializing FileTokenFactory - config of the daemon on whose behalf we run
	private DaemonConfigInfo daemonConfigInfo;

	// Where should we put extra logs (comes from the service/daemon configuration)
	private File logFolder;

	public SgePacket() {
	}

	public SgePacket(final SerializedRequest serializedRequest, final ResourceConfig workerFactoryConfig, final DaemonConfigInfo daemonConfigInfo,
	                 final File logFolder) {
		setSerializedRequest(serializedRequest);
		setWorkerFactoryConfig(workerFactoryConfig);
		setDaemonConfigInfo(daemonConfigInfo);
		setLogFolder(logFolder);
	}

	public Object getWorkPacket() {
		return serializedRequest.getMessage();
	}

	public ResourceConfig getWorkerFactoryConfig() {
		return workerFactoryConfig;
	}

	public void setWorkerFactoryConfig(final ResourceConfig workerFactoryConfig) {
		this.workerFactoryConfig = workerFactoryConfig;
	}

	public SerializedRequest getSerializedRequest() {
		return serializedRequest;
	}

	public void setSerializedRequest(SerializedRequest serializedRequest) {
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

	public void setLogFolder(File logFolder) {
		this.logFolder = logFolder;
	}
}
