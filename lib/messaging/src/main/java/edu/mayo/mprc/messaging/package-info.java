/**
 * Request-response service support.
 * <p>
 * There are two basic concepts to understand about this package:
 * <ul>
 * <li>One request - multiple responses: We send one request to which the service can respond multiple times.</li>
 * <li>One URI to rule them all - we use URI to define not only where, but also how to route the requests.</li>
 * </ul>
 *
 * There is one central concept of a {@link Service}. A service
 * defines where to and how will the request and responses travel. One example is the JMS service, but one could
 * conceive services that use simple TCP socket protocol, or even e-mail based protocol. Each service is uniquely identified by
 * an URI.
 * <p>
 * As this is a test-first development project, the examples are present in the form of unit tests. These use a local JMS
 * broker to perform both sending and receiving.
 * <ul>
 * <li>Just send request, not interested in response: {@link SendReceiveTest#testJmsRequestOnly}</li>
 * <li>Classic request-response: {@link SendReceiveTest#testJmsRequestResponse}</li>
 * <li>Sending multiple messages: {@link SendReceiveTest#testMultipleMessages}</li>
 * </ul>
 * <p>
 * The Service is implemented using JMS.
 * <p>
 * The only class you should use directly is the {@link JmsBrokerThread}. It lets you to start up an embedded
 * JMS message broker.
 * <p>
 * JMS uses a <a href="http://activemq.apache.org/how-should-i-implement-request-response-with-jms.html">technique described here</a>.
 * to implement request-response pattern. We use one response queue per daemon, which is provided by setting
 * daemon name using {@link ServiceFactory#initialize}.
 * <p>
 *
 */
package edu.mayo.mprc.messaging;