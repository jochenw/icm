package com.github.jochenw.icm.core.impl.prop;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.github.jochenw.icm.core.api.prop.IcmPropertyProvider;
import com.github.jochenw.icm.core.impl.prop.DefaultInterpolator;
import com.github.jochenw.icm.core.impl.prop.DefaultIcmPropertyProvider;
import com.github.jochenw.icm.core.util.Exceptions;

public class DefaultInterpolatorTest {
	private DefaultInterpolator interpolator;
	private IcmPropertyProvider propertyProvider;

	@Before
	public void init() {
		propertyProvider = newPropertyProvider();
		interpolator = newInterpolator();
	}
	
	@Test
	public void testInterpolateString() {
		assertFalse(interpolator.isInterpolatable("target"));
		assertTrue(interpolator.isInterpolatable("${targetDir}/test"));
		assertTrue(interpolator.isInterpolatable("${testDir}/db"));
		assertTrue(interpolator.isInterpolatable("hsql:file:${dbDir}/mydb"));
		assertEquals("target", interpolator.interpolate("${targetDir}"));
		assertEquals("target/test", interpolator.interpolate("${targetDir}/test"));
		assertEquals("target/test/db", interpolator.interpolate("${testDir}/db"));
		assertEquals("hsql:file:target/test/db/mydb", interpolator.interpolate("hsql:file:${dbDir}/mydb"));
	}

	@Test
	public void testInterpolateProperties() {
		final Properties props = new Properties();
		props.putAll(propertyProvider.getProperties());
		final Properties interpolatedProps = interpolator.filter(props);
		validate(interpolatedProps);
	}

	public static void validate(final Properties pProps) {
		assertEquals("target", pProps.get("targetDir"));
		assertEquals("target/test", pProps.get("testDir"));
		assertEquals("target/test/db", pProps.get("dbDir"));
		assertEquals("hsql:file:target/test/db/mydb", pProps.get("dbUrl"));
	}

	public static Properties newProperties() {
		final Properties props = new Properties();
		props.put("targetDir", "target");
		props.put("testDir", "${targetDir}/test");
		props.put("dbDir", "${testDir}/db");
		props.put("dbUrl", "hsql:file:${dbDir}/mydb");
		return props;
	}
	
	protected IcmPropertyProvider newPropertyProvider() {
		return new DefaultIcmPropertyProvider(newProperties());
	}

	protected DefaultInterpolator newInterpolator() {
		try {
			final DefaultInterpolator ip = new DefaultInterpolator();
			Field field = DefaultInterpolator.class.getDeclaredField("propertyProvider");
			field.setAccessible(true);
			field.set(ip, propertyProvider);
			return ip;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
