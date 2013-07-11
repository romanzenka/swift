package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.messaging.rmi.MessengerInfo;

/**
 * Class use to send work packet to grid engine. The object contains the actual work itself as {@link #workPacket},
 * but also contains information about how to send progress back from the Sun Grid Engine.
 */
public final class SgePacket {

	private Object workPacket;
	private MessengerInfo messengerInfo;
	private ResourceConfig workerFactoryConfig;
	private DaemonConfigInfo daemonConfigInfo;
	private String sharedTempDirectory;

	public SgePacket() {
	}

	public SgePacket(final Object workPacket, final MessengerInfo messengerInfo, final ResourceConfig workerFactoryConfig, final DaemonConfigInfo daemonConfigInfo) {
		this.workPacket = workPacket;
		this.messengerInfo = messengerInfo;
		this.workerFactoryConfig = workerFactoryConfig;
		this.daemonConfigInfo = daemonConfigInfo;
	}

	public Object getWorkPacket() {
		return workPacket;
	}

	public void setWorkPacket(final Object workPacket) {
		this.workPacket = workPacket;
	}

	public ResourceConfig getWorkerFactoryConfig() {
		return workerFactoryConfig;
	}

	public void setWorkerFactoryConfig(final ResourceConfig workerFactoryConfig) {
		this.workerFactoryConfig = workerFactoryConfig;
	}

	public MessengerInfo getMessengerInfo() {
		return messengerInfo;
	}

	public void setMessengerInfo(final MessengerInfo messengerInfo) {
		this.messengerInfo = messengerInfo;
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
