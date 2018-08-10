package com.github.jochenw.icm.core.api.plugins;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.util.Exceptions;

public class JdbcContext implements LifeCycleAwareCommittable {
	@Inject private JdbcConnectionProvider connectionProvider;
	@InjectLogger private IcmLogger logger;
	private boolean started;
	private Connection connection;
	private final String prefix;

	public JdbcContext(String pPrefix) {
		prefix = pPrefix;
	}

	public Connection getConnection() {
		if (connection == null) {
			try {
				logger.debug("Creating connection for property prefix: " + prefix);
				connection = connectionProvider.getConnection(prefix);
				started = true;
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
		return connection;
	}
	
	@Override
	public void commit() {
		if (connection != null) {
			try {
				logger.debug("Committing connection for property prefix: " + prefix);
				connection.commit();
				connectionProvider.close(connection);
				connection = null;
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}

	@Override
	public void rollback() {
		if (connection != null) {
			try {
				logger.debug("Rollback connection for property prefix: " + prefix);
				connection.rollback();
				connectionProvider.close(connection);
				connection = null;
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}

	@Override
	public void shutdown() {
		try {
			if (started) {
				connectionProvider.shutdown(prefix);
			}
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	public interface ConnectionRunnable {
		public void run(Connection pConnection) throws SQLException;
	}
	public interface ConnectionCallable<T> {
		public T call(Connection pConnection) throws SQLException;
	}

	public <T> T call(ConnectionCallable<T> pCallable) {
		T result = null;
		try {
			Connection conn = getConnection();
			result = pCallable.call(conn);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
		return result;
	}

	public void run(ConnectionRunnable pRunnable) {
		try {
			Connection conn = getConnection();
			pRunnable.run(conn);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}