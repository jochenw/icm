package com.github.jochenw.icm.core.impl.prop;

import java.util.Properties;

import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;

public class DefaultIcmPropertyProvider extends IcmPropertyProvider {
	public DefaultIcmPropertyProvider(Properties pProperties) {
		setProperties(pProperties);
	}
}
