package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.utilities.MonitorUtilities;
import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Server;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestJettyStopThread {
	@Test(timeOut = 10000)
	public void shouldTerminateJetty() throws Exception {
		final Server server = new Server();
		server.addLifeCycleListener(new LifeCycle.Listener() {
			@Override
			public void lifeCycleStarting(final LifeCycle event) {
			}

			@Override
			public void lifeCycleStarted(final LifeCycle event) {
				final JettyStopThread stopThread = new JettyStopThread(server);
				stopThread.start();
				stopThread.getPort();

				MonitorUtilities.sendStopSignal(stopThread.getPort());
			}

			@Override
			public void lifeCycleFailure(final LifeCycle event, final Throwable cause) {
			}

			@Override
			public void lifeCycleStopping(final LifeCycle event) {
			}

			@Override
			public void lifeCycleStopped(final LifeCycle event) {
			}
		});
		server.start();

		server.join();

		Assert.assertTrue(server.isStopped(), "Server did not stop properly");
	}
}
