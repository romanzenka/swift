package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class MessageBrokerValidationTest {

	@Test
	public void validateJMSBrokerTest() {
		MessageBroker broker = null;
		try {
			// Run a broker
			broker = new MessageBroker();
			broker.setBrokerUrl("tcp://localhost:8783");
			broker.start();

			// Check that broker validation passes
			MessageBroker.Config config = new MessageBroker.Config();
			config.setBrokerUrl("tcp://localhost:8783");
			Assert.assertNull(config.validate(), "JMS broker validation failed. Validation should had been successful.");

		} catch (Exception e) {
			throw new MprcException("JMS broker validation test failed", e);
		} finally {
			if (broker != null) {
				broker.stop();
			}
		}
	}

	@Test(dependsOnMethods = {"validateJMSBrokerTest"})
	public void validateJMSBrokerFailedTest() {
		final MessageBroker.Config config = new MessageBroker.Config();
		config.setBrokerUrl("http://localhost:1234");
		Assert.assertNotNull(config.validate(), "JMS broker validation did not fail. Validation should had failed.");
	}
}
