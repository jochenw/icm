package com.github.jochenw.icm.core.api.prop;

import java.util.Properties;

public abstract class IcmPropertyProvider {
	private Properties properties;

	public void setProperties(Properties pProperties) {
		if (properties == null) {
			properties = pProperties;
		} else {
			throw new IllegalStateException("Properties already configured.");
		}
	}

	public Properties getProperties() {
		return properties;
	}

	public String getProperty(String pKey) {
		return properties.getProperty(pKey);
	}
}
