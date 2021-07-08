package com.github.jochenw.icm.core.api;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;


public abstract class AbstractClassExecutionChange implements IcmClassExecutionChange {
	@Inject IComponentFactory componentFactory;

	public IComponentFactory getComponentFactory() { return componentFactory; }
}
