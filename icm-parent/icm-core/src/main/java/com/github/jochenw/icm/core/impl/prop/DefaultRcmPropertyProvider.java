package com.github.jochenw.icm.core.impl.prop;

import java.util.Properties;

import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;

public class DefaultRcmPropertyProvider extends IcmPropertyProvider {
	public DefaultRcmPropertyProvider(Properties pProperties) {
		setProperties(pProperties);
	}
}
