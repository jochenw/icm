package com.github.jochenw.icm.it.test2;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.github.jochenw.icm.core.api.AbstractClassExecutionChange;
import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.ActiveMqContext;
import com.github.jochenw.icm.core.api.plugins.ActiveMqContextProvider;
import com.github.jochenw.icm.core.api.plugins.IcmChange;
import com.github.jochenw.icm.core.util.Exceptions;

@IcmChange(name="ActiveMQ Receiver", version="0.0.3",
             description="Receive some messages from the TestQueue, which has previously been created.")
public class MessageReceiver extends AbstractClassExecutionChange {
	@InjectLogger private IcmLogger logger;

	@Override
	public void run(Context pContext) {
		try {
			final ActiveMqContext ctx = pContext.getContextFor(ActiveMqContextProvider.CONTEXT_ID);
			Session session = ctx.getSession();
			final Queue q = getQueue("TestQueue", ctx.getConnection());
			final MessageConsumer mc = session.createConsumer(q);
			for (int i = 0;  i < 20;  i++) {
				logger.debug("Receiving message #" + i);
				final TextMessage tm = (TextMessage) mc.receive();
				final String expect = "Message #" + i;
				final String got = tm.getText();
				if (!expect.equals(got)) {
					throw new IllegalStateException("Expected " + expect + ", got " + got);
				}
				logger.debug("Received message #" + i);
				tm.acknowledge();
			}
			mc.close();
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	protected Queue getQueue(String pName, Connection pConnection) throws JMSException, NamingException {
		final Properties props = new Properties();
		props.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		props.put(javax.naming.Context.PROVIDER_URL, "tcp://hostname:61616");
		final InitialContext ctx = new InitialContext(props);
		return (Queue) ctx.lookup("dynamicQueues/TestQueue");
	}
}
