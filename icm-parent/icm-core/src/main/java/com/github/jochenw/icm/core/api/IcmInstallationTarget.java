package com.github.jochenw.icm.core.api;

import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;

public interface IcmInstallationTarget<V extends Object> {
	public boolean isInstalled(Context pContext, IcmChangeInfo<V> pResource);
	public void add(Context pContext, IcmChangeInfo<V> pResource);
	int getNumberOfInstalledResources(Context pContext);
}
