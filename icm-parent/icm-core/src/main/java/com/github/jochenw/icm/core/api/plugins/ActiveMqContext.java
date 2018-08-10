package com.github.jochenw.icm.core.api.plugins;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

import com.github.jochenw.icm.core.impl.prop.AbstractPropertyConfigurable;
import com.github.jochenw.icm.core.util.Exceptions;


public class ActiveMqContext extends AbstractPropertyConfigurable implements LifeCycleAwareCommittable {
	private BrokerService broker;
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private final String propertyPrefix;

	public ActiveMqContext(String pPrefix) {
		propertyPrefix = pPrefix;
	}
	
	public BrokerService getBroker() {
		if (broker == null) {
			final boolean embedded = Boolean.parseBoolean(getProperty(propertyPrefix, "embedded"));
			if (embedded) {
				boolean persistent = Boolean.parseBoolean(getProperty(propertyPrefix, "persistent"));
				final BrokerService br;
				try {
					String name = getProperty(propertyPrefix, "brokerName");
					if (name == null) {
						name = "RcmBroker";
					}
					br = new BrokerService();
					br.setBrokerName(name);
					br.setPersistent(persistent);
					br.start();
					broker = br;
				} catch (Throwable t) {
					throw Exceptions.show(t);
				}
			} else {
				return null;
			}
		}
		return broker;
	}
	public ActiveMQConnectionFactory getConnectionFactory() {
		if (connectionFactory == null) {
			getBroker(); // Make sure, that the broker is started.
			final String url = requireProperty(propertyPrefix, "url");
			connectionFactory = new ActiveMQConnectionFactory(url); // "vm://embedded-broker?create=false"
		}
		return connectionFactory;
	}
	public Connection getConnection() {
		if (connection == null) {
			final ConnectionFactory cf = getConnectionFactory();
			try {
				final String userName = getProperty(propertyPrefix, "userName");
				final String password = getProperty(propertyPrefix, "password");
				if (userName == null) {
					connection = cf.createConnection();
				} else {
					connection = cf.createConnection(userName, password);
				}
				connection.start();
			} catch (JMSException e) {
				throw Exceptions.show(e);
			}
		}
		return connection;
	}
	public Session getSession() {
		if (session == null) {
			try {
				final boolean autoAck = Boolean.parseBoolean(getProperty(propertyPrefix, "autoAcknowledge"));
				session = getConnection().createSession(true, autoAck ? Session.AUTO_ACKNOWLEDGE : Session.SESSION_TRANSACTED);
			} catch (JMSException e) {
				throw Exceptions.show(e);
			}
		}
		return session;
	}

	@Override
	public void shutdown() {
		Throwable th = null;
		if (connection != null) {
			try {
				final Connection c = connection;
				connection = null;
				c.close();
			} catch (Throwable t) {
				th = t;
			}
		}
		if (broker != null) {
			try {
				final BrokerService b = broker;
				broker = null;
				b.stop();
			} catch (Throwable t) {
				if (th == null) {
					th = t;
				}
			}
		}
		if (th != null) {
			throw Exceptions.show(th);
		}
	}

	@Override
	public void commit() {
		Throwable th = null;
		try {
			if (session != null) {
				session.commit();
				session = null;
			}
		} catch (Throwable t) {
			th = t;
		}
		if (th != null) {
			throw Exceptions.show(th);
		}
	}

	@Override
	public void rollback() {
		Throwable th = null;
		try {
			if (session != null) {
				final Session s = session;
				session = null;
				s.rollback();
			}
		} catch (Throwable t) {
			th = t;
		}
		if (th != null) {
			throw Exceptions.show(th);
		}
	}

}
