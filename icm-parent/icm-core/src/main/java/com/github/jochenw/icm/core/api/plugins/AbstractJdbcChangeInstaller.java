package com.github.jochenw.icm.core.api.plugins;

import java.sql.Connection;
import java.sql.SQLException;

import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;

public abstract class AbstractJdbcChangeInstaller extends AbstractChangeInstaller  {
	@InjectLogger private IcmLogger logger;

	@Override
	public void install(Context pContext) {
		final String prefix = getPrefix(pContext, "jdbc");
		final JdbcContext jdbcContext = pContext.getContextFor(JdbcContextProvider.CONTEXT_ID + prefix);
		Connection connection = jdbcContext.getConnection();
		try {
			run(pContext, connection);
		} catch (Throwable t) {
			logger.error(t);
		}
	}


	protected String getPrefix(Context pContext, String pDefault) {
		String s = pContext.getInfo().getAttribute("prefix");
		if (s == null ||  s.length() == 0) {
			s = pDefault;
		}
		if (!s.endsWith(".")) {
			return s + ".";
		} else {
			return s;
		}
	}

	protected abstract void run(Context pContext, Connection pConnection) throws SQLException;
}
