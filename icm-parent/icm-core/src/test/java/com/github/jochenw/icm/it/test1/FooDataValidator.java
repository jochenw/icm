package com.github.jochenw.icm.it.test1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.AbstractClassExecutionResource;
import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;
import com.github.jochenw.icm.core.api.plugins.JdbcContext;
import com.github.jochenw.icm.core.api.plugins.JdbcContextProvider;
import com.github.jochenw.icm.core.api.plugins.IcmChange;
import com.github.jochenw.icm.core.api.plugins.SqlStatementExecutor;
import com.github.jochenw.icm.core.util.Exceptions;

@IcmChange(name="FooDataValidator", version="0.0.3",
             description="Validates the data in the FOO table")
public class FooDataValidator extends AbstractClassExecutionResource {
	@Inject private SqlStatementExecutor stmtExecutor;

	@Override
	public void run(Context pContext) {
		final JdbcContext jdbcContext = pContext.getContextFor(JdbcContextProvider.CONTEXT_ID + "jdbc");
		Connection conn = jdbcContext.getConnection();
		try (PreparedStatement pstmt = stmtExecutor.prepare(conn, "SELECT id, name, description FROM FOO");
			 ResultSet rs = pstmt.executeQuery()) {
			int i = 0;
			while (rs.next()) {
				final Long id = rs.getLong(1);
				if (id != i+1) {
					throw new IllegalStateException("Expected id=" + (i+1) + ", got " + id);
				}
				final String name = rs.getString(2);
				if (!("Foo" + i).equals(name)) {
					throw new IllegalStateException("Expected name=Foo" + i + ", got " + name);
				}
				final String description = rs.getString(3);
				final String expectedDescription;
				switch(i++) {
				  case 0:
					  expectedDescription = "A FOO instance";
					  break;
				  case 1:
					  expectedDescription = "Another FOO instance";
					  break;
				  case 2:
					  expectedDescription = "Yet another FOO instance";
					  break;
				  default:
					  throw new IllegalStateException("Invalid index: " + i);
				}
				if (!expectedDescription.equals(description)) {
					throw new IllegalStateException("Expected description=" + expectedDescription
							                        + ", got " + description);
				}
			}
			if (i != 3) {
				throw new IllegalStateException("Expected 3 rows, got " + i);
			}
		} catch (SQLException e) {
			throw Exceptions.show(e);
		}
	}
}
