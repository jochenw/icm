package com.github.jochenw.icm.core.api.plugins;

@ContextProvider
public class JdbcContextProvider extends AbstractContextProvider<JdbcContext> {
	public static final String CONTEXT_ID = "sql:";

	@Override
	public String getContextId() {
		return CONTEXT_ID;
	}

	@Override
	protected String getDefaultPrefix() {
		return "jdbc.";
	}

	@Override
	protected JdbcContext newContext(String pPrefix) {
		return new JdbcContext(pPrefix);
	}
}
