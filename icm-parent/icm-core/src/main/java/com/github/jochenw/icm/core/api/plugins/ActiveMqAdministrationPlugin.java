package com.github.jochenw.icm.core.api.plugins;

import java.io.InputStream;

import javax.jms.Session;

import org.xml.sax.InputSource;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.IcmChangeRepository;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.JaxbUtils;
import com.github.jochenw.icm.core.plugins.schema.activemq.CreateDestination;
import com.github.jochenw.icm.core.plugins.schema.activemq.TDestinationType;


@ResourceInstaller
public class ActiveMqAdministrationPlugin extends AbstractChangeInstaller {
	private static final String fulltype = ActiveMqContextProvider.CONTEXT_ID;
	private static final String type = fulltype.substring(0, fulltype.length()-1);

	@InjectLogger private IcmLogger logger;
	
	@Override
	public boolean isInstallable(IcmChangeInfo<?> pInfo) {
		final String rtype = pInfo.getType();
		return rtype.equals(type)  ||  rtype.startsWith(fulltype);
	}

	@Override
	public void install(Context pContext) {
		final String rtype = pContext.getInfo().getType();
		if (rtype.equals(type)  ||  rtype.startsWith(fulltype)) {
			String prefix = pContext.getInfo().getAttribute("prefix");
			if (prefix == null) {
				prefix = "activemq";
			}
			final ActiveMqContext ctx = pContext.getContextFor(ActiveMqContextProvider.CONTEXT_ID + prefix);
			final Object object = getAdministrationRequest(pContext.getResource(), pContext.getRepository());
			if (object instanceof CreateDestination) {
				createDestination(ctx, (CreateDestination) object);
			} else {
				throw new IllegalStateException("Invalid administration request type: " + object.getClass().getName());
			}
		}
	}

	protected Object getAdministrationRequest(IcmChangeResource pResource, IcmChangeRepository pRepository) {
		try (InputStream in = pRepository.open(pResource)) {
			final InputSource isource = new InputSource(in);
			isource.setSystemId(pResource.getUri());
			return JaxbUtils.parse(isource, CreateDestination.class);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	protected void createDestination(ActiveMqContext pCtx, CreateDestination pDestination) {
		try {
			final Session session = pCtx.getSession();
			final TDestinationType type = pDestination.getType();
			final String name = pDestination.getName();
			switch (type) {
			  case QUEUE:
				logger.debug("Creating queue: " + name);
				session.createQueue(name);
				break;
			  case TOPIC:
				logger.debug("Creating topic: " + name);
				session.createTopic(name);
				break;
			}
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
