package com.github.jochenw.icm.core.api.plugins;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import org.xml.sax.InputSource;

import com.github.jochenw.icm.core.api.plugins.RcmPluginsParser;
import com.github.jochenw.icm.core.api.plugins.XmlPluginRepository;

public class RcmPluginsParserTest {
	private final String PLUGINS_XML_EMPTY =
			"<?xml version='1.0' encoding='UTF-8'?>\n" +
			"<icm-plugins xmlns='" + XmlPluginRepository.NS_1_0_0 + "'>\n" +
			"</icm-plugins>\n";

	private final String PLUGINS_XML_FEW =
			"<?xml version='1.0' encoding='UTF-8'?>\n" +
			"<icm-plugins xmlns='" + XmlPluginRepository.NS_1_0_0 + "'>\n" +
			"  <icm-plugin class='some.class'/>\n" +
			"  <icm-plugin class='another.class'/>\n" +
			"  <icm-plugin class='yet.another.class'/>\n" +
			"</icm-plugins>\n";


	@Test
	public void testXmlEmpty() {
		final List<String> classNames = new ArrayList<>();
		final Consumer<String> classNameConsumer = (s) -> classNames.add(s);
		final InputSource source = new InputSource(new StringReader(PLUGINS_XML_EMPTY));
		new RcmPluginsParser().parse(source, classNameConsumer);
		assertTrue(classNames.isEmpty());
	}

	@Test
	public void testXmlFew() {
		final List<String> classNames = new ArrayList<>();
		final Consumer<String> classNameConsumer = (s) -> classNames.add(s);
		final InputSource source = new InputSource(new StringReader(PLUGINS_XML_FEW));
		new RcmPluginsParser().parse(source, classNameConsumer);
		assertEquals(3, classNames.size());
		assertEquals("some.class", classNames.get(0));
		assertEquals("another.class", classNames.get(1));
		assertEquals("another.class", classNames.get(1));
	}

}
