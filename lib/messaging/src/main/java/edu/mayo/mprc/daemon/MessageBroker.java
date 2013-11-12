package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.*;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.io.IOException;

/**
 * This module runs an embedded message broker at given URL.
 * <p/>
 * The broker is being set as non-persistent so we do not have to deal with the broker creating temporary
 * files. If the user wants permanent broker, they can install standalone ActiveMQ and configure it separately.
 */
public final class MessageBroker implements Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(MessageBroker.class);
	public static final String TYPE = "messageBroker";
	public static final String NAME = "Message Broker";
	public static final String DESC = "Daemons need a JMS (Java Messaging Service) message broker to communicate with each other. The broker can be external or embedded within Swift. It is enough for one of the daemons to define access to the broker. All other daemons then connect to it using the URI you configure.</p><p>A more robust alternative is to use an external broker. Swift was tested with Apache ActiveMQ 5.2.0, which can be obtained at <a href=\"http://activemq.apache.org/\">http://activemq.apache.org/</a>. To use external broker, download, configure and run it, fill in the broker URI and uncheck the 'Run embedded broker' checkbox.</p>";

	private String brokerUrl;
	private BrokerService broker;
	private boolean embedded;
	private boolean useJmx;

	private static final String EMBEDDED = "embedded";
	private static final String USE_JMX = "useJmx";
	public static final String BROKER_URL = "brokerUrl";
	private static final String EMBEDDED_BROKER_URL = "embeddedBrokerUrl";

	public MessageBroker() {
		embedded = true;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public void setBrokerUrl(final String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(final boolean embedded) {
		this.embedded = embedded;
	}

	public boolean isUseJmx() {
		return useJmx;
	}

	public void setUseJmx(final boolean useJmx) {
		this.useJmx = useJmx;
	}

	/**
	 * The broker is running either when it is embdeded, and it is actually running,
	 * or if it is not embedded - then it is assumed to be running without our intervention.
	 */
	@Override
	public boolean isRunning() {
		return broker != null || !isEmbedded();
	}

	@Override
	public void start() {
		if (broker == null && isEmbedded()) {
			broker = new BrokerService();
			try {
				broker.addConnector(getBrokerUrl());
				broker.setPersistent(false);
				broker.setUseJmx(isUseJmx());
				broker.start();
			} catch (Exception e) {
				throw new MprcException("The message broker failed to start", e);
			}
		}
	}

	@Override
	public void stop() {
		if (broker != null && isEmbedded()) {
			try {
				broker.stop();
			} catch (Exception e) {
				// SWALLOWED: failing to stop the broker is not a life/death problem
				LOGGER.error("Could not stop the embedded message broker", e);
			}
		}
	}

	public void deleteAllMessages() {
		if (isRunning()) {
			try {
				broker.deleteAllMessages();
			} catch (IOException e) {
				throw new MprcException("Failed to clean messages in the broker", e);
			}
		}
	}

	/**
	 * A factory capable of creating the resource
	 */
	public static final class Factory extends FactoryBase<Config, MessageBroker> implements FactoryDescriptor {
		@Override
		public MessageBroker create(final Config config, final DependencyResolver dependencies) {
			final MessageBroker broker = new MessageBroker();

			broker.setEmbedded(config.isEmbedded());
			broker.setUseJmx(config.isUseJmx());

			broker.setBrokerUrl(config.effectiveBrokerUrl());

			return broker;
		}

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public String getUserName() {
			return NAME;
		}

		@Override
		public String getDescription() {
			return DESC;
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return new Ui();
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig, NamedResource {
		private String brokerUrl;
		private String embeddedBrokerUrl;
		private String embedded;
		private String useJmx;

		public Config() {
		}

		public static Config getEmbeddedBroker() {
			final Config config = new Config();
			config.brokerUrl = "vm://broker";
			config.embeddedBrokerUrl = "vm://broker";
			config.embedded = "true";
			config.useJmx = "false";

			return config;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(BROKER_URL, getBrokerUrl(), "URL of the broker");
			writer.put(EMBEDDED, getEmbedded(), "Should we run the embedded broker?");
			writer.put(EMBEDDED_BROKER_URL, getEmbeddedBrokerUrl(), "ActiveMQ configuration URL defining how to start the embedded broker up (if embedded)");
			writer.put(USE_JMX, getUseJmx(), "Enable JMX on the broker");
		}

		@Override
		public void load(final ConfigReader reader) {
			brokerUrl = reader.get(BROKER_URL);
			embeddedBrokerUrl = reader.get(EMBEDDED_BROKER_URL);
			embedded = reader.get(EMBEDDED);
			useJmx = reader.get(USE_JMX);
		}

		@Override
		public int getPriority() {
			return 10;
		}

		public String getBrokerUrl() {
			return brokerUrl;
		}

		public void setBrokerUrl(final String brokerUrl) {
			this.brokerUrl = brokerUrl;
		}

		public void setEmbeddedBrokerUrl(final String embeddedBrokerUrl) {
			this.embeddedBrokerUrl = embeddedBrokerUrl;
		}

		public String getEmbeddedBrokerUrl() {
			return embeddedBrokerUrl;
		}

		public void setEmbedded(String embedded) {
			this.embedded = embedded;
		}

		public String getEmbedded() {
			return embedded;
		}

		public String getUseJmx() {
			return useJmx;
		}

		public boolean isUseJmx() {
			return getUseJmx() != null && getUseJmx().equalsIgnoreCase("true");
		}

		public boolean isEmbedded() {
			return getEmbedded() != null && getEmbedded().equalsIgnoreCase("true");
		}

		public String validate() {
			Connection connection = null;
			try {
				if (brokerUrl != null && !brokerUrl.isEmpty()) {
					final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
					connection = connectionFactory.createConnection();
				} else {
					return "JMS broker URL is not valid.";
				}
			} catch (JMSException e) {
				return "JMS broker connection could not be established Error: " + e.getMessage();
			} finally {
				try {
					if (connection != null) {
						connection.close();
					}
				} catch (JMSException e) {
					//SWALLOWED
					LOGGER.warn("Error closing JMS broker connection.", e);
				}
			}
			return null;
		}

		public String effectiveBrokerUrl() {
			if (isEmbedded() && !getEmbeddedBrokerUrl().trim().isEmpty()) {
				return getEmbeddedBrokerUrl().trim();
			} else {
				return getBrokerUrl().trim();
			}
		}

		@Override
		public String getName() {
			return "messageBroker";
		}

		@Override
		public void setName(final String name) {
			if (!"messageBroker".equals(name)) {
				throw new MprcException("The message broker is a singleton. Only one can exist, and it must be named 'messageBroker'");
			}
		}
	}

	public static final class Ui implements ServiceUiFactory {

		public static final String DEFAULT_PORT = "61616";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(BROKER_URL, "Connection URI", "The URI defines where the broker runs (hostname and port) as well as the protocol used to communicate with it." +
					" The initial default value is set to use failover. This settign will allow for reconnection attempts if JMS broker system goes down.")
					.required().defaultValue(getDefaultBrokerUrl(daemon))
					.validateOnDemand(new PropertyChangeListener() {
						@Override
						public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
							if (!(config instanceof Config)) {
								ExceptionUtilities.throwCastException(config, Config.class);
								return;
							}
							final Config brokerConfig = (Config) config;
							final String error = brokerConfig.validate();
							response.displayPropertyError(config, propertyName, error);
						}

						@Override
						public void fixError(final ResourceConfig config, final String propertyName, final String action) {
						}
					})

					.property(EMBEDDED, "Run embedded broker",
							"When this field is checked, Swift will run its own broker within the daemon. The configuration will be taken from the connection URI."
									+ " Embedded ActiveMQ 5.2.0 can take a multitude of URI formats."
									+ " Check out <a href=\"http://activemq.apache.org/uri-protocols.html\">http://activemq.apache.org/uri-protocols.html</a> for several tips."
									+ " We have experimented mostly with the <tt>tcp://host:port</tt> protocol. "
									+ "<p>Note: When running the embedded broker, we manually switch off persistence. URIs that enable persistence may fail.</p>"
									+ " <p>When this field is unchecked, we assume that external broker is already running at the given URI.</p>")
					.required().boolValue().defaultValue("true").enable(EMBEDDED_BROKER_URL, true)

					.property(EMBEDDED_BROKER_URL, "Embedded broker URI",
							"The URI defines embedded JMS broker system. This URI may be different from the connection URI, for example, connection URI may have "
									+ "failover configuration options while this URI will not.").defaultValue("tcp://" + daemon.getHostName() + ":" + DEFAULT_PORT)
					.defaultValue("tcp://" + daemon.getHostName() + ":" + DEFAULT_PORT)

					.property(USE_JMX, "Enable the use of JMX", "").boolValue().defaultValue("false");
		}

		public static String getDefaultBrokerUrl(final DaemonConfig daemon) {
			return "failover:(tcp://" + daemon.getHostName() + ":" + DEFAULT_PORT + ")";
		}
	}
}
