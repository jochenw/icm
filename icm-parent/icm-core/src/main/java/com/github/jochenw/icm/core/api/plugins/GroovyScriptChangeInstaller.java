package com.github.jochenw.icm.core.api.plugins;

import java.io.Reader;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.util.Exceptions;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

@ResourceInstaller
public class GroovyScriptChangeInstaller extends AbstractChangeInstaller {
	public static final String CONTEXT_ID = "groovy";

	@Override
	public boolean isInstallable(IcmChangeInfo<?> pInfo) {
		return CONTEXT_ID.equals(pInfo.getType());
	}

	@Override
	public void install(Context pContext) {
		final Binding binding = new Binding();
		final GroovyShell shell = new GroovyShell(binding);
		binding.setProperty("context", pContext);
		try (Reader r = pContext.openText()) {
			shell.evaluate(r);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
