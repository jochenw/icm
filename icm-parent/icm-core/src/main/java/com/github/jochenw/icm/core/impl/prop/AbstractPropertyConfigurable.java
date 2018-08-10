package com.github.jochenw.icm.core.impl.prop;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;

public class AbstractPropertyConfigurable {
	@Inject private IcmPropertyProvider propertyProvider;

	protected String requireProperty(final String pPrefix, String pKey) {
		if (pPrefix.endsWith(".")) {
			return requireProperty(pPrefix + pKey);
		} else {
			return requireProperty(pPrefix + "." + pKey);
		}
	}

	protected String getProperty(final String pPrefix, String pKey) {
		if (pPrefix.endsWith(".")) {
			return getProperty(pPrefix + pKey);
		} else {
			return getProperty(pPrefix + "." + pKey);
		}
	}

	protected String requireProperty(final String pKey) {
		final String value = propertyProvider.getProperty(pKey);
		if (value == null  ||  value.length() == 0) {
			throw new IllegalStateException("Missing, or empty, property: " + pKey);
		}
		return value;
	}

	protected String getProperty(final String pKey) {
		final String value = propertyProvider.getProperty(pKey);
		return value;
	}
}
