package com.github.jochenw.icm.core.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.icm.core.Tests;
import com.github.jochenw.icm.core.api.IcmChangeResource;


public class DirectoryRepositoryTest {
	@Test
	public void test() {
		final IComponentFactory cf = ClassPathResourceRepositoryTest.newComponentFactory();
		final DirectoryRepository dr = new DirectoryRepository(new File("."), null);
		cf.init(dr);
		final List<IcmChangeResource> resources = new ArrayList<>();
		dr.list((r) -> resources.add(r));
		Tests.assertResource(resources, "DirectoryRepository.java", "src/main/java/com/github/jochenw/icm/core/impl/DirectoryRepository.java");
		Tests.assertResource(resources, "DirectoryRepositoryTest.java", "src/test/java/com/github/jochenw/icm/core/impl/DirectoryRepositoryTest.java");
		Tests.assertResource(resources, "pom.xml", "pom.xml");
	}
}
