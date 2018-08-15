package com.github.jochenw.icm.it.test2;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.github.jochenw.icm.core.api.AbstractClassExecutionChange;
import com.github.jochenw.icm.core.api.IcmPluginContext.Committable;
import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.plugins.ActiveMqContext;
import com.github.jochenw.icm.core.api.plugins.ActiveMqContextProvider;
import com.github.jochenw.icm.core.api.plugins.IcmChange;
import com.github.jochenw.icm.core.util.Exceptions;

@IcmChange(name="ActiveMQ Sender", version="0.0.2",
             description="Send some messages to the TestQueue, which has previously been created.")
public class MessageSender extends AbstractClassExecutionChange {
	@InjectLogger private IcmLogger logger;

	@Override
	public void run(Context pContext) {
		try {
			final ActiveMqContext ctx = pContext.getContextFor(ActiveMqContextProvider.CONTEXT_ID);
			Session session = ctx.getSession();
			final Queue q = getQueue("TestQueue", ctx.getConnection());
			final MessageProducer mp = session.createProducer(q);
			mp.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			for (int i = 0;  i< 20;  i++) {
				logger.debug("Sending message #" + i);
				final TextMessage tm = session.createTextMessage("Message #" + i);
				mp.send(tm);
			}
			mp.close();
			for (Committable c : pContext.getTransactions()) {
				c.commit();
			}
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
