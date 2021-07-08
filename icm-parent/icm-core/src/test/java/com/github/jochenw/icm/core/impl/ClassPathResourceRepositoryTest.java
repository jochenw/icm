package com.github.jochenw.icm.core.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.Test;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Binder;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.inject.simple.SimpleComponentFactoryBuilder;
import com.github.jochenw.icm.core.Tests;
import com.github.jochenw.icm.core.api.DefaultIcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmBuilder;
import com.github.jochenw.icm.core.api.IcmChangeNumber;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.impl.log.SimpleLoggerFactory;

public class ClassPathResourceRepositoryTest {
	@Test
	public void test() throws Exception {
		final IComponentFactory cf = newComponentFactory();
		final Predicate<String> filter = (s) -> s.endsWith(".class");
		final ClassPathResourceRepository<IcmChangeNumber> cpr = new ClassPathResourceRepository<IcmChangeNumber>(filter, StandardCharsets.UTF_8);
		cf.init(cpr);
		
		final List<IcmChangeResource> resources = new ArrayList<>();
		cpr.list((r) -> resources.add(r));
		Tests.assertResource(resources, "Test.class", "org/junit/Test.class");
		Tests.assertResource(resources, "Logger.class", "org/apache/logging/log4j/Logger.class");
		Tests.assertResource(resources, "ClassPathResourceRepository.class", "com/github/jochenw/icm/core/impl/ClassPathResourceRepository.class");
		Tests.assertResource(resources, "ClassPathResourceRepositoryTest.class", "com/github/jochenw/icm/core/impl/ClassPathResourceRepositoryTest.class");
	}

	public static IComponentFactory newComponentFactory() {
		final SimpleComponentFactoryBuilder cbf = new SimpleComponentFactoryBuilder();
		cbf.onTheFlyBinder(new IcmBuilder.IcmOnTheFlyBinder());
		final Module module = new Module() {
			@Override
			public void configure(Binder pBinder) {
				pBinder.bind(IcmLoggerFactory.class).toInstance(new SimpleLoggerFactory(System.err, IcmLogger.Level.DEBUG));
				pBinder.bind(IcmChangeNumberHandler.class).toInstance(new DefaultIcmChangeNumberHandler());
				pBinder.bind(ClassLoader.class).toInstance(Thread.currentThread().getContextClassLoader());;
			}
		};
		return cbf.module(module).build();
	}
}
