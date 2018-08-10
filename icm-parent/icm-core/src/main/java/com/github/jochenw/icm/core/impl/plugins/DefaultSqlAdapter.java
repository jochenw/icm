package com.github.jochenw.icm.core.impl.plugins;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.jochenw.icm.core.api.plugins.SqlAdapter;


public class DefaultSqlAdapter implements SqlAdapter {
	@Override
	public boolean isTableExisting(Connection pConnection, String pSchemaName, String pTableName) throws SQLException {
		DatabaseMetaData dbmd = pConnection.getMetaData();
		try (ResultSet rs = dbmd.getTables(null, pSchemaName, pTableName, new String[] {"TABLE"})) {
			if (rs.next()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public SelectBuilder createSelect(final String pSchemaName, final String pTableName) {
		final List<String> resultColumns = new ArrayList<>();
		final List<String> whereColumns = new ArrayList<>();
		return new SelectBuilder() {

			@Override
			public SelectBuilder resultColumn(String pColumn, String pAlias) {
				resultColumns.add(pColumn);
				resultColumns.add(pAlias);
				return this;
			}

			@Override
			public SelectBuilder whereColumn(String pColumn, String pCondition) {
				whereColumns.add(pColumn);
				whereColumns.add(pCondition);
				return this;
			}

			@Override
			public String build() {
				return createSelect(pSchemaName, pTableName, resultColumns, whereColumns);
			}
		};
	}

	@Override
	public CreateTableBuilder createCreateTable(final String pSchemaName, final String pTableName) {
		return new CreateTableBuilder() {
			final List<String> columns = new ArrayList<>();
			final List<String[]> uniqueIndexes = new ArrayList<>();

			@Override
			public CreateTableBuilder idColumn(String pColumnName) {
				columns.add(pColumnName);
				columns.add("ID");
				return this;
			}

			@Override
			public CreateTableBuilder stringColumn(String pColumnName, int pLength) {
				columns.add(pColumnName);
				columns.add("STRING:" + pLength);
				return this;
			}

			@Override
			public CreateTableBuilder dateTimeColumn(String pColumnName) {
				columns.add(pColumnName);
				columns.add("DATETIME");
				return this;
			}

			@Override
			public CreateTableBuilder unique(String... pColumnNames) {
				uniqueIndexes.add(pColumnNames);
				return this;
			}

			@Override
			public String[] build() {
				return createCreateTable(pSchemaName, pTableName, columns, uniqueIndexes);
			}
		};
	}

	@Override
	public InsertBuilder createInsert(String pSchemaName, String pTableName) {
		return new InsertBuilder() {
			private final List<String> columns = new ArrayList<>();

			@Override
			public InsertBuilder valueColumn(String pColumnName) {
				columns.add(pColumnName);
				return this;
			}

			@Override
			public String build() {
				return createInsert(pSchemaName, pTableName, columns);
			}
		};
	}

	protected String createInsert(String pSchemaName, String pTableName, List<String> pColumns) {
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ");
		if (pSchemaName != null) {
			sb.append(pSchemaName);
			sb.append('.');
		}
		sb.append(pTableName);
		sb.append(" (");
		for (int i = 0;  i < pColumns.size();  i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(pColumns.get(i));
		}
		sb.append(") VALUES (");
		for (int i = 0;  i < pColumns.size();  i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append("?");
		}
		sb.append(")");
		return sb.toString();
	}

	protected String[] createCreateTable(String pSchemaName, String pTableName, List<String> pColumns,
			                             List<String[]> pUniqueIndexes) {
		final StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ");
		if (pSchemaName != null) {
			sb.append(pSchemaName);
			sb.append('.');
		}
		sb.append(pTableName);
		sb.append(" (");
		for (int i = 0;  i < pColumns.size();  i += 2) {
			final String col = pColumns.get(i);
			final String type = pColumns.get(i);
			if (i > 0) {
				sb.append(", ");
			}
			switch(type) {
			  case "ID":
				sb.append(col);
				sb.append(" BIGINT NOT NULL PRIMARY KEY");
				break;
			  case "DATETIME":
				sb.append(col);
				sb.append(" TIMESTAMP WITH TIMEZONE NOT NULL");
				break;
			  default:
				 if (type.startsWith("STRING:")) {
					 final int len = Integer.parseInt(type.substring("STRING:".length()));
					 sb.append(col);
					 sb.append(" VARCHAR(");
					 sb.append(len);
					 sb.append(") NOT NULL");
				 } else {
					 throw new IllegalStateException("Invalid column type: " + type);
				 }
				 break;
			}
		}
		for (int i = 0;  i < pUniqueIndexes.size();  i++) {
			final String[] cols = pUniqueIndexes.get(i);
			sb.append(", UNIQUE (");
			for (int j = 0;  j < cols.length;  j++) {
				if (j > 0) {
					sb.append(", ");
				}
				sb.append(cols[i]);
			}
		}
		sb.append(")");
		return new String[] {sb.toString()};

	}

	protected String createSelect(String pSchemaName, String pTableName, List<String> pResultColumns,
			                      List<String> pWhereColumns) {
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		if (pResultColumns.isEmpty()) {
			sb.append("*");
		} else {
			for (int i = 0;  i < pResultColumns.size();  i += 2) {
				final String col = pResultColumns.get(i);
				final String alias = pResultColumns.get(i+1);
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(col);
				if (alias != null) {
					sb.append("AS ");
					sb.append(alias);
				}
			}
		}
		sb.append(" FROM ");
		if (pSchemaName != null) {
			sb.append(pSchemaName);
			sb.append('.');
		}
		sb.append(pTableName);
		if (!pWhereColumns.isEmpty()) {
			for (int i = 0;  i < pWhereColumns.size();  i += 2) {
				final String col = pWhereColumns.get(i);
				final String cond = pWhereColumns.get(i+1);
				if (i == 0) {
					sb.append(" WHERE ");
				} else {
					sb.append(" AND ");
				}
				sb.append(col);
				sb.append(" ");
				sb.append(cond);
				sb.append(" ?");
			}
		}
		return sb.toString();
	}
}
