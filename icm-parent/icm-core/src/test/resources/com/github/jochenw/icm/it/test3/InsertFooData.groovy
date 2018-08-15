/* @IcmChange(name="Insert Foo Data", type="groovy", version="0.0.2", description="Insert some test data into the FOO table.")
 */

import groovy.sql.Sql;
import com.github.jochenw.icm.core.api.plugins.JdbcContext;
import com.github.jochenw.icm.core.api.plugins.JdbcContextProvider
 
def jdbcContext = context.getContextFor(JdbcContextProvider.CONTEXT_ID + "jdbc");
def sql = new Sql(jdbcContext.getConnection());
def insertSql = "INSERT INTO FOO (id, name, description) VALUES (?, ?, ?)";
sql.executeInsert(insertSql, [1, "Foo0", "A FOO instance"]);
sql.executeInsert(insertSql, [2, "Foo1", "Another FOO instance"]);
sql.executeInsert(insertSql, [3, "Foo2", "Yet another FOO instance"]);
 