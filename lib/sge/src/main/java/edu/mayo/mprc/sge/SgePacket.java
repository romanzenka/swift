package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.messaging.SerializedRequest;

/**
 * Class use to send work packet to grid engine. The object contains the actual work itself as {@link #workPacket},
 * but also contains information about how to send progress back from the Sun Grid Engine.
 */
public final class SgePacket {

	private Object workPacket;
	private SerializedRequest serializedRequest;
	private ResourceConfig workerFactoryConfig;
	private DaemonConfigInfo daemonConfigInfo;
	private String sharedTempDirectory;

	public SgePacket() {
	}

	public SgePacket(final SerializedRequest serializedRequest, final ResourceConfig workerFactoryConfig, final DaemonConfigInfo daemonConfigInfo) {
		this.serializedRequest = serializedRequest;
		this.workerFactoryConfig = workerFactoryConfig;
		this.daemonConfigInfo = daemonConfigInfo;
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

	public String getSharedTempDirectory() {
		return sharedTempDirectory;
	}

	public void setSharedTempDirectory(final String sharedTempDirectory) {
		this.sharedTempDirectory = sharedTempDirectory;
	}
}
