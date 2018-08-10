package com.github.jochenw.icm.core.api.plugins;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionProvider {
	public Connection getConnection(String pPrefix) throws SQLException;
	public void close(Connection pConnection) throws SQLException;
	public void shutdown(String pPrefix) throws SQLException;
}
