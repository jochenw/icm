package com.github.jochenw.icm.core.api;

public interface IcmChangeInfoProvider {
	<T> IcmChangeInfo<T> getInfo(IcmChangeResource pResource, IcmChangeRepository pRepository);
}
