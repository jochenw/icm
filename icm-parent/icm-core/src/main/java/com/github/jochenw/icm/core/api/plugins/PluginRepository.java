package com.github.jochenw.icm.core.api.plugins;

import java.util.ArrayList;
import java.util.List;

import com.github.jochenw.icm.core.api.IcmChangeInfoProvider;
import com.github.jochenw.icm.core.api.IcmContextProvider;
import com.github.jochenw.icm.core.api.IcmChangeInstaller;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.util.Exceptions;

public abstract class PluginRepository {
	private List<Object> plugins = new ArrayList<Object>();
	private List<IcmChangeInfoProvider> resourceInfoProviders = new ArrayList<IcmChangeInfoProvider>();
	private List<IcmChangeInstaller> resourceInstallers = new ArrayList<>();
	private List<IcmContextProvider> contextProviders = new ArrayList<>();
	@InjectLogger IcmLogger logger;

	protected PluginRepository() {
	}

	public void setLoggerFactory(IcmLoggerFactory pLoggerFactory) {
		logger = pLoggerFactory.getLogger(PluginRepository.class);
		init();
	}
	
	protected abstract void init();

	protected void addPluginClass(Class<?> pType) {
		try {
			final Object o = pType.newInstance();
			if (pType.isAnnotationPresent(ResourceInfoProvider.class)) {
				resourceInfoProviders.add((IcmChangeInfoProvider) o);
			} else if (pType.isAnnotationPresent(ResourceInstaller.class)) {
				resourceInstallers.add((IcmChangeInstaller) o);
			} else if (pType.isAnnotationPresent(ContextProvider.class)) {
				contextProviders.add((IcmContextProvider) o);
			} else {
				throw new IllegalStateException("Unable to determin plugin type for " + pType.getName());
			}
			plugins.add(o);
		} catch (Throwable t) {
			logger.error("Failed to instantiate class: " + pType.getName(), t);
			throw Exceptions.show(t);
		}
			
	}

	public List<IcmChangeInfoProvider> getResourceInfoProviders() {
		return resourceInfoProviders;
	}

	public List<IcmChangeInstaller> getResourceInstallers() {
		return resourceInstallers;
	}

	public List<IcmContextProvider> getContextProviders() {
		return contextProviders;
	}
}
