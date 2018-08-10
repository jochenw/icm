package com.github.jochenw.icm.core.api.plugins;

import java.sql.Connection;
import java.sql.SQLException;

public interface SqlAdapter {
	public interface SelectBuilder {
		default SelectBuilder resultColumn(String pColumn) { return resultColumn(pColumn, null); }
		SelectBuilder resultColumn(String pColumn, String pAlias);
		SelectBuilder whereColumn(String pColumn, String pCondition);
		String build();
	}
	public interface CreateTableBuilder {
		CreateTableBuilder idColumn(String pColumnName);
		CreateTableBuilder stringColumn(String pColumnName, int pLength);
	    CreateTableBuilder dateTimeColumn(String string);
		CreateTableBuilder unique(String... pColumnNames);
		String[] build();
	}
	public interface InsertBuilder {
		InsertBuilder valueColumn(String string);
		String build();
	}
	public boolean isTableExisting(Connection pConnection, String pSchemaName, String pTableName) throws SQLException;
	public default boolean isTableExisting(Connection pConnection, String pTableName) throws SQLException { return isTableExisting(pConnection, null, pTableName); }
	public SelectBuilder createSelect(String pSchemaName, String pTableName);
	public default SelectBuilder createSelect(String pTableName) { return createSelect(null, pTableName); }
	public CreateTableBuilder createCreateTable(String pSchemaName, String pTableName);
	public default CreateTableBuilder createCreateTable(String pTableName) { return createCreateTable(null, pTableName); }
	public InsertBuilder createInsert(String pSchemaName, String pTableName);
	public default InsertBuilder createInsert(String pTableName) { return createInsert(null, pTableName); }
}
