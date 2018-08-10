package com.github.jochenw.icm.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public interface IcmChangeInstaller {
	public interface Context extends IcmPluginContext {
		public IcmChangeResource getResource();
		public IcmChangeRepository getRepository();
		public IcmChangeInfo<?> getInfo();
		InputStream open() throws IOException;
		Reader openText() throws IOException;
	}
	public boolean isInstallable(IcmChangeInfo<?> pInfo);
	public void install(Context pContext);
}
