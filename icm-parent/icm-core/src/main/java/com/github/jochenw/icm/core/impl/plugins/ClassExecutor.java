package com.github.jochenw.icm.core.impl.plugins;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmClassExecutionChange;
import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.plugins.AbstractChangeInstaller;
import com.github.jochenw.icm.core.api.plugins.ResourceInstaller;
import com.github.jochenw.icm.core.util.Exceptions;

@ResourceInstaller
public class ClassExecutor extends AbstractChangeInstaller {
	@Inject private ComponentFactory componentFactory;
	@Inject private ClassLoader classLoader;

	@Override
	public boolean isInstallable(IcmChangeInfo<?> pInfo) {
		return pInfo.getType().startsWith("class:");
	}

	@Override
	public void install(Context pContext) {
		final String type = pContext.getInfo().getType();
		if (!type.startsWith("class:")) {
			throw new IllegalStateException("Invalid type: " + type);
		}
		final String className = type.substring("class:".length());
		final Class<?> clazz = loadClass(className);
		final IcmClassExecutionChange rcer = newExecutionResource(clazz);
		run(rcer, pContext);
	}

	protected Class<?> loadClass(String pClassName) {
		try {
			return classLoader.loadClass(pClassName);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	protected IcmClassExecutionChange newExecutionResource(Class<?> pClass) {
		try {
			final IcmClassExecutionChange rcer = (IcmClassExecutionChange) pClass.newInstance();
			componentFactory.init(rcer);
			return rcer;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	protected void run(IcmClassExecutionChange pRcer, Context pContext) {
		try {
			pRcer.run(pContext);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
