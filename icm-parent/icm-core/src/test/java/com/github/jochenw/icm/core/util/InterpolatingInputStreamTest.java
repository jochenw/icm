package com.github.jochenw.icm.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.github.jochenw.icm.core.util.InterpolatingInputStream;
import com.github.jochenw.icm.core.util.Streams;

public class InterpolatingInputStreamTest {
	private Properties properties;

	@Before
	public void init() {
		properties = com.github.jochenw.icm.core.impl.prop.DefaultInterpolatorTest.newProperties();
	}

	@Test
	public void test() throws Exception {
		final Charset cs = StandardCharsets.UTF_8;
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (OutputStreamWriter osw = new OutputStreamWriter(baos, cs)) {
			properties.store(osw, null);
		}
		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		final Function<String,String> f = (s) -> properties.getProperty(s);
		final InterpolatingInputStream in = new InterpolatingInputStream(bais, f, cs);
		final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
		Streams.copy(in, baos2);
		final String interpolatedPropertiesString = new String(baos2.toByteArray(), cs);
		final Properties interpolatedProps = new Properties();
		interpolatedProps.load(new StringReader(interpolatedPropertiesString));
		com.github.jochenw.icm.core.impl.prop.DefaultInterpolatorTest.validate(interpolatedProps);
	}

}
