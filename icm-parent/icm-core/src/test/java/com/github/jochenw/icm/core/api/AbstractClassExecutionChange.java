package com.github.jochenw.icm.core.api;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.IcmClassExecutionChange;
import com.github.jochenw.icm.core.api.cf.ComponentFactory;

public abstract class AbstractClassExecutionChange implements IcmClassExecutionChange {
	@Inject ComponentFactory componentFactory;

	public ComponentFactory getComponentFactory() { return componentFactory; }
}
