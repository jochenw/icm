package com.github.jochenw.icm.core.api;

import com.github.jochenw.icm.core.api.plugins.LifeCycleAware;

public interface IcmContextProvider extends LifeCycleAware {
	public boolean isContextProviderFor(String pId);
	public <C extends LifeCycleAware> C getContextFor(IcmPluginContext pContext, String pId);
}
