package edu.mayo.mprc.messaging;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;

import static org.mockito.Mockito.*;

/**
 * Code needs to be able to decide it wants to receive messages from a sender that does not exist yet.
 * It needs to create and pass along a token, that enables creation of the sender on the other side.
 *
 * @author Roman Zenka
 */
public final class ResponderFirstTest extends MessagingTestBase {
	@BeforeClass
	public void init() {
		startBroker();
	}

	@Test
	public void shouldSerializeRequests() throws InterruptedException {
		Serializable message = new String("hello");
		ResponseListener listener = mock(ResponseListener.class);

		// Serialize the request, then deserialize it right back
		SerializedRequest serializedRequest = serviceFactory.serializeRequest(message, listener);
		Request request = serviceFactory.deserializeRequest(serializedRequest);

		String receivedMessage = (String) request.getMessageData();
		Assert.assertEquals(receivedMessage, message);

		request.sendResponse("all is well", true);
		request.processed();

		verify(listener, timeout(20)).responseReceived("all is well", true);
	}
}
