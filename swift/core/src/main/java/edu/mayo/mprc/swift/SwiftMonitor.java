package edu.mayo.mprc.swift;

import com.google.common.collect.Lists;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.DaemonConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contains links to all daemon connections defined to be able to monitor everything.
 * Pings them periodically and provides a list of daemon statuses.
 *
 * @author Roman Zenka
 */
public final class SwiftMonitor implements Runnable {
	/**
	 * Guarded by {@link #connectionsLock}
	 */
	private final Collection<DaemonConnection> monitoredConnections = new ArrayList<DaemonConnection>(20);
	public static final long MONITOR_PERIOD_SECONDS = 30L;

	private MultiFactory factory;
	private DependencyResolver dependencies;
	private ScheduledExecutorService scheduler;
	private final Object connectionsLock = new Object();

	public SwiftMonitor() {
	}

	public void initialize(final ApplicationConfig app) {
		synchronized (connectionsLock) {
			for (DaemonConfig daemonConfig : app.getDaemons()) {
				for (ServiceConfig serviceConfig : daemonConfig.getServices()) {
					final DaemonConnection daemonConnection = (DaemonConnection) getFactory().createSingleton(serviceConfig, getDependencies());
					monitoredConnections.add(daemonConnection);
				}
			}
		}
	}

	/**
	 * Start monitoring Swift.
	 */
	public void start() {
		if (scheduler == null) {
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(this, MONITOR_PERIOD_SECONDS, MONITOR_PERIOD_SECONDS, TimeUnit.SECONDS);
		}
	}

	/**
	 * Stop monitoring Swift.
	 */
	public void stop() {
		scheduler.shutdown();
	}

	public void ping() {
		List<DaemonConnection> copy;
		synchronized (connectionsLock) {
			copy = Lists.newArrayList(monitoredConnections);
		}
		for (final DaemonConnection connection : copy) {
			connection.ping();
		}
	}

	/**
	 * @return Copy of the list of monitored connections. Includes the status information.
	 */
	public List<DaemonConnection> getMonitoredConnections() {
		synchronized (connectionsLock) {
			return Lists.newArrayList(monitoredConnections);
		}
	}

	public MultiFactory getFactory() {
		return factory;
	}

	public void setFactory(MultiFactory factory) {
		this.factory = factory;
	}

	public DependencyResolver getDependencies() {
		return dependencies;
	}

	public void setDependencies(DependencyResolver dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public void run() {
		ping();
	}
}
