package com.github.jochenw.icm.core.api.plugins;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.icm.core.api.IcmContextProvider;
import com.github.jochenw.icm.core.api.IcmPluginContext;
import com.github.jochenw.icm.core.util.Exceptions;


@ContextProvider
public abstract class AbstractContextProvider<C extends LifeCycleAwareCommittable> implements IcmContextProvider {
	@Inject private IComponentFactory componentFactory;
	private Map<String,C> contextMap = new HashMap<>();

	public abstract String getContextId();
	protected abstract String getDefaultPrefix();
	protected abstract C newContext(String pPrefix);

	protected String getQContextId() {
		final String ctxId = getContextId();
		if (ctxId.endsWith(":")) {
			return ctxId;
		} else {
			return ctxId + ":";
		}
	}

	public IComponentFactory getComponentFactory()  {
		return componentFactory;
	}
	
	@Override
	public boolean isContextProviderFor(String pId) {
		return pId.startsWith(getQContextId());
	}

	@SuppressWarnings("unchecked")
	@Override
	public C getContextFor(IcmPluginContext pContext, String pId) {
		final StringBuilder sb = new StringBuilder();
		final String ctxId = getQContextId();
		if (pId.startsWith(ctxId)) {
			sb.append(pId.substring(ctxId.length()));
		} else {
			throw new IllegalStateException("Invalid id: " + pId);
		}
		if (sb.length() == 0) {
			sb.append(getDefaultPrefix());
		}
		if (sb.charAt(sb.length()-1) != '.') {
			sb.append('.');
		}
		final String prefix = sb.toString();
		C c = contextMap.get(prefix);
		if (c == null) {
			c = newContext(prefix);
			componentFactory.init(c);
			contextMap.put(prefix, c);
			pContext.add(c);
		}
		return c;
	}

	public void shutdown() {
		Throwable th = null;
		for (C c : contextMap.values()) {
			try {
				c.shutdown();
			} catch (Throwable t) {
				if (th == null) {
					th = t;
				}
			}
		}
		if (th != null) {
			throw Exceptions.show(th);
		}
	}
}
