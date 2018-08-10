package com.github.jochenw.icm.core.impl;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

import com.github.jochenw.icm.core.api.plugins.Attribute;
import com.github.jochenw.icm.core.api.plugins.IcmChange;
import com.github.jochenw.icm.core.impl.AsmClassInfoProvider;
import com.github.jochenw.icm.core.impl.AsmClassInfoProvider.ClassInfo;

public class AsmClassInfoProviderTest {
	@IcmChange(name="A test resource", version="2.2.0", description="A description",
	             attributes={@Attribute(name="Attribute0", value="Value0"), @Attribute(name="Attribute1", value="Value1")})
	public static class TestResource {
	}

	@Test
	public void testTestResourceClass() throws Exception {
		final AsmClassInfoProvider infoProvider = new AsmClassInfoProvider();
		final URL url = TestResource.class.getResource("AsmClassInfoProviderTest$TestResource.class");
		assertNotNull(url);
		try (InputStream in = url.openStream()) {
			final ClassInfo classInfo = infoProvider.getClassInfo(in);
			assertNotNull(classInfo);
			assertEquals(TestResource.class.getName(), classInfo.getQName());
			assertEquals(TestResource.class.getSimpleName(), classInfo.getSimpleName());
			assertEquals("A test resource", classInfo.getResourceName());
			assertEquals("class:" + TestResource.class.getName(), classInfo.getResourceType());
			assertEquals("2.2.0", classInfo.getResourceVersion());
			assertEquals("A description", classInfo.getResourceDescription());
			final Map<String,String> attributes = classInfo.getAttributes();
			assertNotNull(attributes);
			assertEquals(2, attributes.size());
			assertEquals("Value0", attributes.get("Attribute0"));
			assertEquals("Value1", attributes.get("Attribute1"));
		}
	}

}
