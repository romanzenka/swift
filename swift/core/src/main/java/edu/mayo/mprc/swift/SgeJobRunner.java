package edu.mayo.mprc.swift;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.daemon.DaemonProgress;
import edu.mayo.mprc.daemon.DaemonProgressMessage;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import edu.mayo.mprc.messaging.Request;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.sge.SgePacket;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public final class SgeJobRunner {

	private static final Logger LOGGER = Logger.getLogger(SgeJobRunner.class);
	private ResourceTable resourceTable;
	private ActiveMQConnectionPool connectionPool;
	private ServiceFactory serviceFactory;

	public SgeJobRunner() {
	}

	/**
	 * Takes a name with work packet serialized into xml. Executes the work packet and communicates the results.
	 *
	 * @param workPacketXmlFile File containing the serialized work packet.
	 */
	public void run(final File workPacketXmlFile) {
		// Wait for the work packet to fully materialize in case it was transferred over a shared filesystem
		FileUtilities.waitForFileBlocking(workPacketXmlFile);

		FileInputStream fileInputStream = null;
		SgePacket sgePacket = null;

		try {
			LOGGER.info("Running grid job in host: " + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			LOGGER.error("Could not get host name.", e);
		}

		Request request = null;
		try {
			LOGGER.debug(ReleaseInfoCore.buildVersion());
			LOGGER.info("Parsing xml file: " + workPacketXmlFile.getAbsolutePath());

			final XStream xStream = new XStream(new DomDriver());

			fileInputStream = new FileInputStream(workPacketXmlFile);

			sgePacket = (SgePacket) xStream.fromXML(fileInputStream);
			serviceFactory.initialize(sgePacket.getSerializedRequest().getBrokerUri(), null);
			request = serviceFactory.deserializeRequest(sgePacket.getSerializedRequest());

			//If the work packet is an instance of a FileTokenHolder, set the the FileTokenFactory on it. The FileTokenFactory object
			//needs to be reset because it is a transient object.
			if (sgePacket.getWorkPacket() instanceof FileTokenHolder) {
				final FileTokenHolder fileTokenHolder = (FileTokenHolder) sgePacket.getWorkPacket();
				final FileTokenFactory fileTokenFactory = new FileTokenFactory(sgePacket.getDaemonConfigInfo());

				if (sgePacket.getSharedTempDirectory() != null) {
					fileTokenFactory.setTempFolderRepository(new File(sgePacket.getSharedTempDirectory()));
				}

				fileTokenHolder.translateOnReceiver(fileTokenFactory, null);
			}

			final DependencyResolver dependencies = new DependencyResolver(resourceTable);
			final Worker daemonWorker = (Worker) resourceTable.createSingleton(sgePacket.getWorkerFactoryConfig(), dependencies);
			daemonWorker.processRequest((WorkPacket) sgePacket.getWorkPacket(), new DaemonWorkerProgressReporter(request));
		} catch (Exception e) {
			final String errorMessage = "Failed to process work packet " + ((sgePacket == null || sgePacket.getWorkPacket() == null) ? "null" : sgePacket.getWorkPacket().toString());
			LOGGER.error(errorMessage, e);

			try {
				reportProgress(request, e);
			} catch (RemoteException ex) {
				LOGGER.error("Error sending exception " + MprcException.getDetailedMessage(e) + " to GridRunner", ex);
				// SWALLOWED
			}

			FileUtilities.closeQuietly(getConnectionPool());
			System.exit(1);
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					LOGGER.warn("Error closing file input stream.", e);
					// SWALLOWED
				}
			}
		}

		LOGGER.info("Work packet " + sgePacket.getWorkPacket().toString() + " successfully processed.");
		FileUtilities.closeQuietly(getConnectionPool());
		System.exit(0);
	}

	public ResourceTable getResourceTable() {
		return resourceTable;
	}

	public void setResourceTable(final ResourceTable resourceTable) {
		this.resourceTable = resourceTable;
	}

	public ActiveMQConnectionPool getConnectionPool() {
		return connectionPool;
	}

	public void setConnectionPool(ActiveMQConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	private static void reportProgress(final Request request, final Serializable serializable) throws RemoteException {
		request.sendResponse(serializable, false);
	}

	class DaemonWorkerProgressReporter implements ProgressReporter {
		private Request request;

		DaemonWorkerProgressReporter(final Request request) {
			this.request = request;
		}

		@Override
		public void reportStart(final String hostString) {
			try {
				SgeJobRunner.reportProgress(request, new DaemonProgressMessage(hostString));
			} catch (Exception t) {
				try {
					SgeJobRunner.reportProgress(request, t);
				} catch (RemoteException ex) {
					LOGGER.error("Error sending exception " + MprcException.getDetailedMessage(t) + " to GridRunner", ex);
					// SWALLOWED
				}
				FileUtilities.closeQuietly(getConnectionPool());
				System.exit(1);
			}
		}

		@Override
		public void reportProgress(final ProgressInfo progressInfo) {
			try {
				SgeJobRunner.reportProgress(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo, progressInfo));
			} catch (RemoteException e) {
				LOGGER.error("Error reporting daemon worker progress.", e);
				//SWALLOWED
			}
		}

		@Override
		public void reportSuccess() {
			//Do nothing. GridRunner gets notified of completion by SGE.
		}

		@Override
		public void reportFailure(final Throwable t) {
			try {
				SgeJobRunner.reportProgress(request, t);
			} catch (RemoteException e) {
				LOGGER.error("Error reporting daemon worker failure.", e);
				//SWALLOWED
			}
		}
	}
}
