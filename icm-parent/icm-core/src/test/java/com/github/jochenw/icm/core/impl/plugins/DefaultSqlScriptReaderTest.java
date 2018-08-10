package com.github.jochenw.icm.core.impl.plugins;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Iterator;

import org.junit.Test;

import com.github.jochenw.icm.core.impl.plugins.DefaultSqlScriptReader;

public class DefaultSqlScriptReaderTest {
	private static final DefaultSqlScriptReader dssr = new DefaultSqlScriptReader();
	private static final String INSERT = "INSERT INTO FOO (a, b, c) VALUES (?, ?, ?)";
	private static final String CREATE1 = "CREATE TABLE FOO (\n"
			+ "  a BIGINT NOT NULL PRIMARY KEY,\n"
			+ "  b VARCHAR(20) NOT NULL,\n"
			+ "  c VARCHAR(10)\n"
			+ ")";
	private static final String CREATE2 = CREATE1.replace("VARCHAR(10)", "VARCHAR(10)-- Embedded comment");
	private static final String CREATE2_EXPECT = CREATE2.replace("-- Embedded comment", "");
	private static final String DROP = "DROP TABLE FOO;";
	private static final String MULTI_STATEMENT_SCRIPT = "-- Comment 0\n"
			+ CREATE1 + ";\n"
			+ "-- Comment 1\n"
			+ "-- Comment 2\n"
			+ DROP + ";\n"
			+ "-- Comment 3\n"
			+ CREATE2 + ";\n"
			+ "-- Comment 4\n"
			+ INSERT + ";\n"
			+ "-- Comment 5\n"
			+ "-- Comment 6\n";

	@Test
	public void testSingleLineScript() {
		final String script = INSERT + ";\n";
		runTest(script, INSERT);
	}

	@Test
	public void testMultiLineScript() {
		final String script = CREATE1 + ";\n";
		runTest(script, CREATE1);
}

	@Test
	public void testEmbeddedComment() {
		final String script = CREATE2 + ";\n";
		runTest(script, CREATE2_EXPECT);
	}

	@Test
	public void testMultipleStatements() {
		runTest(MULTI_STATEMENT_SCRIPT, CREATE1, DROP, CREATE2_EXPECT, INSERT);
	}

	protected void runTest(String pScript, String... pStatements) {
		runTest(pScript, pStatements, false);
	}
	protected void runTest(String pScript, String[] pStatements, boolean pCheckReaderClosed) {
		final MyStringReader msr1 = new MyStringReader(pScript);
		final Iterator<String> iter1 = dssr.read(msr1);
		validate(iter1, pStatements);
		if (pCheckReaderClosed) {
			assertTrue(msr1.isClosed());
		}

		final MyStringReader msr2 = new MyStringReader(pScript.replaceAll("\\n", "\r\n"));
		final Iterator<String> iter2 = dssr.read(msr2);
		validate(iter2, pStatements);
		if (pCheckReaderClosed) {
			assertTrue(msr2.isClosed());
		}
	}

	protected void validate(Iterator<String> pIter, String[] pStatements)  {
		for (int i = 0;  i < pStatements.length;  i++) {
			final String msg = String.valueOf(i) + ": " + pStatements[i];
			assertTrue(msg, pIter.hasNext());
			assertEquals(msg, pStatements[i], pIter.next());
		}
		assertFalse(pIter.hasNext());
	}

	public static class MyStringReader extends StringReader {
		public MyStringReader(String s) {
			super(s);
		}
		private boolean closed;
		@Override
		public void close() {
			super.close();
			closed = true;
		}
		public boolean isClosed() {
			return closed;
		}
	}
	
	@Test
	public void testCheckReaderClosed() {
		final String script1 = INSERT + "\n";
		runTest(script1, new String[] {INSERT}, true);
		final String script2 = CREATE1 + "\n";
		runTest(script2, new String[] {CREATE1}, true);
		final String script3 = CREATE2 + "\n";
		runTest(script3, new String[] {CREATE2.replace("-- Embedded comment", "")}, true);
		final String script4 = MULTI_STATEMENT_SCRIPT;
		runTest(script4, new String[] {CREATE1, DROP, CREATE2_EXPECT, INSERT}, true);
	}
}
