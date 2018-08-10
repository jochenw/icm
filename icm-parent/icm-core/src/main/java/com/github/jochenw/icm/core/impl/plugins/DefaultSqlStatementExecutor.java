package com.github.jochenw.icm.core.impl.plugins;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.SqlStatementExecutor;

public class DefaultSqlStatementExecutor implements SqlStatementExecutor {
	@InjectLogger private IcmLogger logger; 

	@Override
	public void execute(Connection pConnection, String pStatement) throws SQLException {
		logger.debug(pStatement);
		try (PreparedStatement stmt = pConnection.prepareStatement(pStatement)) {
			stmt.executeUpdate();
		}
	}

	@Override
	public PreparedStatement prepare(Connection pConnection, String pStatement) throws SQLException {
		logger.debug(pStatement);
		return pConnection.prepareStatement(pStatement);
	}
}
