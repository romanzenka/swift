package edu.mayo.mprc.filesharing.jms;

import edu.mayo.mprc.messaging.ActiveMQConnectionPool;

import javax.jms.Connection;
import java.net.URI;

public final class JmsFileTransferHandlerFactory {

	private URI brokerUri;
	private String userName;
	private String password;
	private ActiveMQConnectionPool connectionPool;

	public JmsFileTransferHandlerFactory() {
	}

	public JmsFileTransferHandlerFactory(final ActiveMQConnectionPool connectionPool, final URI brokerUri) {
		this.connectionPool = connectionPool;
		this.brokerUri = brokerUri;
	}

	public JmsFileTransferHandlerFactory(final ActiveMQConnectionPool connectionPool, final URI brokerUri, final String userName, final String password) {
		this(connectionPool, brokerUri);
		this.userName = userName;
		this.password = password;
	}

	public URI getBrokerUri() {
		return brokerUri;
	}

	public void setBrokerUri(final URI brokerUri) {
		this.brokerUri = brokerUri;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public JmsFileTransferHandler createFileSharing(final String sourceId) {
		final Connection connection = connectionPool.getConnectionToBroker(brokerUri, userName, password);
		return new JmsFileTransferHandler(connection, sourceId);
	}
}
