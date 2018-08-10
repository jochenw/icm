package com.github.jochenw.icm.core.api.log;


public abstract class IcmLoggerFactory {
	public abstract IcmLogger getLogger(String pId);
	public IcmLogger getLogger(Class<?> pClass) { return getLogger(pClass.getName()); }
}
