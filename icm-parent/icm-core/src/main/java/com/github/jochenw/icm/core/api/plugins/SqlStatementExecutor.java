package com.github.jochenw.icm.core.api.plugins;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SqlStatementExecutor {
	public void execute(Connection pConnection, String pStatement) throws SQLException;
	public PreparedStatement prepare(Connection pConnection, String pStatement) throws SQLException;
}
