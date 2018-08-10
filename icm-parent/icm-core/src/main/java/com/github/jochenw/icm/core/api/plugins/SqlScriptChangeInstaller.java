package com.github.jochenw.icm.core.api.plugins;

import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.util.Exceptions;

@ResourceInstaller
public class SqlScriptChangeInstaller extends AbstractJdbcChangeInstaller {
	@Inject private SqlScriptReader sqlScriptReader;
	@Inject private SqlStatementExecutor sqlStatementExecutor;
	@InjectLogger private IcmLogger logger;

	@Override
	public boolean isInstallable(IcmChangeInfo<?> pInfo) {
		return "sql".equals(pInfo.getType());
	}

	@Override
	protected void run(Context pContext, Connection pConnection) throws SQLException {
		try (final Reader reader = pContext.openText()) {
			final Iterator<String> iter = sqlScriptReader.read(reader);
			while (iter.hasNext()) {
				final String stmt = iter.next();
				sqlStatementExecutor.execute(pConnection, stmt);
				if (!pConnection.getAutoCommit()) {
					pConnection.commit();
				}
			}
		} catch (Throwable t) {
			throw Exceptions.show(t, SQLException.class);
		}
	}
}
