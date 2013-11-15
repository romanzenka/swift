package edu.mayo.mprc.launcher;

import edu.mayo.mprc.utilities.MonitorUtilities;
import org.mortbay.jetty.Server;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestJettyStopThread {
	@Test(timeOut = 10000)
	public void shouldTerminateJetty() throws Exception {
		Server server = new Server(38080);
		server.start();
		Thread.sleep(300);
		Assert.assertTrue(server.isStarted(), "Server did not start properly");

		JettyStopThread stopThread = new JettyStopThread(server);
		stopThread.start();
		stopThread.getPort();

		MonitorUtilities.sendStopSignal(stopThread.getPort());

		server.join();

		Assert.assertTrue(server.isStopped(), "Server did not stop properly");
	}
}
