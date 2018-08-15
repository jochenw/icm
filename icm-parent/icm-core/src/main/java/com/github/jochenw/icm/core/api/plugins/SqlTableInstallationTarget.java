package com.github.jochenw.icm.core.api.plugins;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.jms.IllegalStateException;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmInstallationTarget;
import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;
import com.github.jochenw.icm.core.api.plugins.JdbcContext.ConnectionCallable;
import com.github.jochenw.icm.core.api.plugins.JdbcContext.ConnectionRunnable;
import com.github.jochenw.icm.core.util.Exceptions;

public class SqlTableInstallationTarget<V> implements IcmInstallationTarget<V> {
	@Inject private SqlAdapter sqlAdapter;
	@Inject private IcmChangeNumberHandler<V> changeNumberHandler;
	String propertyPrefix;
	String schemaName, tableName;

	public void setPropertyPrefix(String pPropertyPrefix) {
		propertyPrefix = pPropertyPrefix;
	}

	public void setSchemaName(String pSchemaName) {
		schemaName = pSchemaName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setTableName(String pTableName) {
		tableName = pTableName;
	}

	public String getTableName() {
		if (tableName == null) {
			return "ICM_CHANGES";
		} else {
			return tableName;
		}
	}
	
	public String getPropertyPrefix() {
		if (propertyPrefix == null) {
			return "jdbc";
		} else {
			return propertyPrefix;
		}
	}

	@Override
	public boolean isInstalled(Context pContext, IcmChangeInfo<V> pResource) {
		final ConnectionCallable<Boolean> callable = new ConnectionCallable<Boolean>() {
			@Override
			public Boolean call(Connection pConnection) throws SQLException {
				if (sqlAdapter.isTableExisting(pConnection, getSchemaName(), getTableName())) {
					return Boolean.FALSE;
				} else {
					final String selectSql = sqlAdapter.createSelect(getSchemaName(), getTableName())
							                           .resultColumn("id")
							                           .whereColumn("change_number", "=")
							                           .whereColumn("name", "=")
							                           .whereColumn("type", "=")
							                           .build();
					try (PreparedStatement stmt = pConnection.prepareStatement(selectSql)) {
						stmt.setString(1, changeNumberHandler.asString(pResource.getVersion()));
						stmt.setString(2, pResource.getTitle());
						stmt.setString(3, pResource.getType());
						try (ResultSet rs = stmt.executeQuery()) {
							return rs.next() ? Boolean.TRUE : Boolean.FALSE;
						}
					}
				}
			}
			
		};
		return call(pContext, callable).booleanValue();
	}

	@Override
	public void add(Context pContext, IcmChangeInfo<V> pResource) {
		final ConnectionRunnable runnable = new ConnectionRunnable() {
			@Override
			public void run(Connection pConnection) throws SQLException {
				long id;
				if (sqlAdapter.isTableExisting(pConnection, getSchemaName(), getTableName())) {
					final String[] createSql = sqlAdapter.createCreateTable(getSchemaName(), getTableName())
							.idColumn("id")
							.stringColumn("change_number", 64)
							.stringColumn("name", 128)
							.stringColumn("type", 64)
							.dateTimeColumn("installation_time")
							.unique("change_number", "name", "type")
							.build();
					id = 1;
					for (String s : createSql) {
						try (PreparedStatement stmt = pConnection.prepareStatement(s)) {
							stmt.executeUpdate();
						}
					}
					insert(pConnection, 0, "0.0.0", "$ICM$-Schema", "sql");
				} else {
					final String selectSql = sqlAdapter.createSelect(getSchemaName(), getTableName())
							.resultColumn("MAX(id)", "max_id")
							.build();
					try (PreparedStatement stmt = pConnection.prepareStatement(selectSql);
							ResultSet rs = stmt.executeQuery()) {
						if (!rs.next()) {
							id = 1;
						} else {
							id = rs.getLong(1)+1;
						}
					}
				}
				insert(pConnection, id, changeNumberHandler.asString(pResource.getVersion()),
						pResource.getTitle(), pResource.getType());
			}
		};
		run(pContext, runnable);
	}

	protected void insert(Connection pConnection, long pId, String pChangeNumber, String pName,
			              String pType) throws SQLException {
		final String insertSql = sqlAdapter.createInsert(getSchemaName(), getTableName())
                .valueColumn("id")
                .valueColumn("change_number")
                .valueColumn("name")
                .valueColumn("type")
                .valueColumn("installation_date")
                .build();
		try (PreparedStatement stmt = pConnection.prepareStatement(insertSql)) {
			stmt.setLong(1, pId);
			stmt.setString(2, pChangeNumber);
			stmt.setString(3, pName);
			stmt.setString(4, pType);
			final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			stmt.setTimestamp(5, new Timestamp(cal.getTime().getTime()), cal);
			stmt.executeUpdate();
		}
	}
	
	@Override
	public int getNumberOfInstalledResources(Context pContext) {
		final ConnectionCallable<Integer> consumer = new ConnectionCallable<Integer>() {
			@Override
			public Integer call(Connection pConnection) throws SQLException {
				try {
					if (sqlAdapter.isTableExisting(pConnection, getSchemaName(), getTableName())) {
						return Integer.valueOf(0);
					} else {
						final String selectSql = sqlAdapter.createSelect(getSchemaName(), getTableName())
								.resultColumn("COUNT(id)", "cnt")
								.whereColumn("id", "!=")
								.build();
						try (PreparedStatement stmt = pConnection.prepareStatement(selectSql)) {
							stmt.setString(1, "0");
							try (ResultSet rs = stmt.executeQuery()) {
								if (rs.next()) {
									return Integer.valueOf(rs.getInt(1)+1);
								} else {
									throw new IllegalStateException("Expected result row");
								}
							}
						}
					}
				} catch(Throwable t) {
					throw Exceptions.show(t);
				}
			}
		};
		return call(pContext, consumer).intValue();
	}

	protected JdbcContext getJdbcContext(Context pContext) {
		final String prefix = getPropertyPrefix();
		return pContext.getContextFor(JdbcContextProvider.CONTEXT_ID + prefix);
	}

	protected void run(Context pContext, ConnectionRunnable pRunnable) {
		getJdbcContext(pContext).run(pRunnable);
	}

	protected <T> T call(Context pContext, ConnectionCallable<T> pCallable) {
		return getJdbcContext(pContext).call(pCallable);
	}
}
