package com.github.jochenw.icm.core.api.plugins;

@ContextProvider
public class ActiveMqContextProvider extends AbstractContextProvider<ActiveMqContext> {
	public static final String CONTEXT_ID = "activemq:";

	@Override
	public String getContextId() {
		return CONTEXT_ID;
	}

	@Override
	protected String getDefaultPrefix() {
		return "activemq.";
	}

	@Override
	protected ActiveMqContext newContext(String pPrefix) {
		final ActiveMqContext ctx = new ActiveMqContext(pPrefix);
		return ctx;
	}

}
