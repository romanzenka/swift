package edu.mayo.mprc.messaging.rmi;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Class handles the registering of remotes object to an local RMI registry
 * and getting remote object from remote registries.
 * @deprecated This would not work in firewalled environment.
 */
public final class RemoteObjectHandler {
	private static final Logger LOGGER = Logger.getLogger(RemoteObjectHandler.class);

	private Registry localRegistry;
	private int localPort;

	private final Object monitor = new Object();

	public void registerRemoteObject(final String remoteObjectName, final Remote remoteObject) throws RemoteException {
		synchronized (monitor) {
			getLocalRegistry().rebind(remoteObjectName, UnicastRemoteObject.exportObject(remoteObject, 0));
		}
	}

	public void unregisterRemoteObject(final String remoteObjectName) throws RemoteException, NotBoundException {
		synchronized (monitor) {
			getLocalRegistry().unbind(remoteObjectName);
		}
	}

	public Remote getRemoteObject(final InetSocketAddress registryInfo, final String remoteObjectName) throws RemoteException, NotBoundException {
		return LocateRegistry.getRegistry(registryInfo.getHostName(), registryInfo.getPort()).lookup(remoteObjectName);
	}

	private Registry getLocalRegistry() throws RemoteException {
		if (!isLocalRegistryValid()) {
			final AnonymousRMISocketFactory serverAnonymousRMISocketFactory = new AnonymousRMISocketFactory();
			final AnonymousRMISocketFactory clientAnonymousRMISocketFactory = new AnonymousRMISocketFactory();
			localRegistry = LocateRegistry.createRegistry(localPort, clientAnonymousRMISocketFactory, serverAnonymousRMISocketFactory);

			localPort = serverAnonymousRMISocketFactory.getLastUsedPort();

			LOGGER.info("RMI Registry created and listening on port: " + localPort);
		}

		return localRegistry;
	}

	public int getLocalPort() throws RemoteException {
		synchronized (monitor) {
			if (isLocalRegistryValid()) {
				getLocalRegistry();
			}
		}

		return localPort;
	}

	public String getHost() throws UnknownHostException {
		return InetAddress.getLocalHost().getHostName();
	}

	public InetSocketAddress getLocalRegistryInfo() throws UnknownHostException, RemoteException {
		return new InetSocketAddress(InetAddress.getLocalHost().getHostName(), getLocalPort());
	}

	public String[] listlocalRemoteObjectName() throws RemoteException {
		synchronized (monitor) {
			return getLocalRegistry().list();
		}
	}

	private boolean isLocalRegistryValid() {
		try {
			if (localRegistry != null) {
				localRegistry.list();
				return true;
			}
		} catch (Exception e) {
			// SWALLOWED
		}

		return false;
	}
}
