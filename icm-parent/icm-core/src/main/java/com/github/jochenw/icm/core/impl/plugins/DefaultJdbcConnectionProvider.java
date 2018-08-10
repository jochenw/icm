package com.github.jochenw.icm.core.impl.plugins;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.JdbcConnectionProvider;
import com.github.jochenw.icm.core.api.plugins.SqlStatementExecutor;
import com.github.jochenw.icm.core.impl.prop.AbstractPropertyConfigurable;
import com.github.jochenw.icm.core.util.Exceptions;

public class DefaultJdbcConnectionProvider extends AbstractPropertyConfigurable implements JdbcConnectionProvider {
	@Inject private ClassLoader classLoader;
	@Inject private SqlStatementExecutor statementExecutor;
	@InjectLogger private IcmLogger logger;

	@Override
	public Connection getConnection(String pPrefix) throws SQLException {
		final String url = requireProperty(pPrefix + "url");
		final String createDir = getProperty(pPrefix + "createDir");
		if (createDir != null  &&  createDir.length() != 0) {
			final File f = new File(createDir);
			if (f.isDirectory()) {
				logger.debug("getConnection: Db directory exists: " + f.getAbsolutePath());
			} else {
				logger.debug("getConnection: Creating db directory: " + f.getAbsolutePath());
				if (!f.mkdirs()) {
					throw new IllegalStateException("Unable to create directory: " + f.getAbsolutePath());
				}
			}
		}
		return getConnection(url, pPrefix);
	}

	protected Connection getConnection(String pUrl, String pPrefix) throws SQLException {
		final String driver = requireProperty(pPrefix + "driver");
		final String userName = requireProperty(pPrefix + "userName");
		final String password = requireProperty(pPrefix + "password");
		final String autoCommitStr = getProperty(pPrefix + "autoCommit");
		final boolean autoCommit = autoCommitStr == null ? false : Boolean.parseBoolean(autoCommitStr);
		
		try {
			classLoader.loadClass(driver);
		} catch (Throwable t) {
			logger.error("Unable to load driver class " + driver + " via ClassLoader " + classLoader, t);
			throw Exceptions.show(t);
		}
		try {
			final Connection conn = DriverManager.getConnection(pUrl, userName, password);
			if (autoCommit) {
				conn.setAutoCommit(true);
			}
			return conn;
		} catch (Throwable t) {
			logger.error("Unable to connect to database URL " + pUrl + " as " + userName +
					     ((password == null  ||  password.length() == 0) ? " without password" : " using password <NOTLOGGED>"), t);
			throw Exceptions.show(t, SQLException.class);
		}
	}
	
	@Override
	public void close(Connection pConnection) throws SQLException {
		pConnection.close();
	}

	@Override
	public void shutdown(String pPrefix) {
		final String shutdownUrl = getProperty(pPrefix + "shutdownUrl");
		final String shutdownStatement = getProperty(pPrefix + "shutdownStatement");
		if (shutdownUrl != null  &&  shutdownUrl.length() > 0) {
			try (Connection conn = getConnection(shutdownUrl, pPrefix)) {
				
			} catch (SQLException e) {
				if (40000 == e.getErrorCode()  &&  "XJ004".equals(e.getSQLState())) {
					// Workaround for Apache Derby throwing an Exception, if database wasn't started.
					// Ignore this.
				} else if (45000 == e.getErrorCode()  &&  "08006".equals(e.getSQLState())) {
					// Workaround for Apache Derby throwing an Exception, if database wasn't started.
					// Ignore this.
				} else {
					throw Exceptions.show(e);
				}
				
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		} else if (shutdownStatement != null  &&  shutdownStatement.length() > 0) {
			try (Connection conn = getConnection(pPrefix)) {
				statementExecutor.execute(conn, shutdownStatement);
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}
}
