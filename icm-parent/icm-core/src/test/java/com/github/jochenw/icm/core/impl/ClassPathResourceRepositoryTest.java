package com.github.jochenw.icm.core.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.Test;

import com.github.jochenw.icm.core.Tests;
import com.github.jochenw.icm.core.api.DefaultIcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeNumber;
import com.github.jochenw.icm.core.api.IcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeResource;
import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.cf.ComponentFactoryBuilder;
import com.github.jochenw.icm.core.api.cf.ComponentFactoryBuilder.Binder;
import com.github.jochenw.icm.core.api.cf.ComponentFactoryBuilder.Module;
import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.impl.ClassPathResourceRepository;
import com.github.jochenw.icm.core.impl.log.SimpleLoggerFactory;

public class ClassPathResourceRepositoryTest {
	@Test
	public void test() throws Exception {
		final ComponentFactory cf = newComponentFactory();
		final Predicate<String> filter = (s) -> s.endsWith(".class");
		final ClassPathResourceRepository<IcmChangeNumber> cpr = new ClassPathResourceRepository<IcmChangeNumber>(filter, StandardCharsets.UTF_8) {
			@Override
			protected void accept(Consumer<IcmChangeResource> pConsumer, IcmChangeResource pResource) {
				pConsumer.accept(pResource);
			}
		};
		cf.init(cpr);
		
		final List<IcmChangeResource> resources = new ArrayList<>();
		cpr.list((r) -> resources.add(r));
		Tests.assertResource(resources, "Test.class", "org/junit/Test.class");
		Tests.assertResource(resources, "Logger.class", "org/apache/logging/log4j/Logger.class");
		Tests.assertResource(resources, "ClassPathResourceRepository.class", "com/github/jochenw/icm/core/impl/ClassPathResourceRepository.class");
		Tests.assertResource(resources, "ClassPathResourceRepositoryTest.class", "com/github/jochenw/icm/core/impl/ClassPathResourceRepositoryTest.class");
	}

	public static ComponentFactory newComponentFactory() {
		final ComponentFactoryBuilder cbf = new ComponentFactoryBuilder();
		final Module module = new Module() {
			@Override
			public void bind(Binder pBinder) {
				pBinder.bindInstance(IcmLoggerFactory.class, new SimpleLoggerFactory(System.err, IcmLogger.Level.DEBUG));
				pBinder.bindInstance(IcmChangeNumberHandler.class, new DefaultIcmChangeNumberHandler());
				pBinder.bindInstance(ClassLoader.class, Thread.currentThread().getContextClassLoader());;
			}
		};
		return cbf.module(module).build();
	}
}
