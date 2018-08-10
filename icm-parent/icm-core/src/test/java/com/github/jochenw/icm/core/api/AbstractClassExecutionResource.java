package com.github.jochenw.icm.core.api;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.RcmClassExecutionResource;
import com.github.jochenw.icm.core.api.cf.ComponentFactory;

public abstract class AbstractClassExecutionResource implements RcmClassExecutionResource {
	@Inject ComponentFactory componentFactory;

	public ComponentFactory getComponentFactory() { return componentFactory; }
}
