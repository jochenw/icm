package com.github.jochenw.icm.core.api.plugins;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.icm.core.api.IcmChangeInstaller;
import com.github.jochenw.icm.core.api.cf.InjectLogger;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;

public abstract class AbstractChangeInstaller implements IcmChangeInstaller {
	@Inject private IComponentFactory componentFactory;
	@Inject private IcmPropertyProvider propertyProvider;
	@InjectLogger private IcmLogger logger;

	protected IComponentFactory getComponentFactory() {
		return componentFactory;
	}

	protected IcmPropertyProvider getPropertyProvider() {
		return propertyProvider;
	}

	protected String getProperty(String pKey) {
		return getPropertyProvider().getProperty(pKey);
	}

	protected String requireProperty(String pKey) {
		final String value = getProperty(pKey);
		if (value == null  ||  value.length() == 0) {
			throw new IllegalStateException("Missing, or empty, property: " + pKey);
		}
		return value;
	}
}
