package com.github.jochenw.icm.core.api.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.jochenw.icm.core.util.Exceptions;

public class XmlPluginRepository extends PluginRepository {
	public static String NS_1_0_0 = "http://namespaces.github.com/jochenw/icm/icm-plugins/1.0.0";
	@Inject ClassLoader classLoader;

	protected ClassLoader getClassLoader() {
		return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
	}
	protected void init() {
		Enumeration<URL> en;
		try {
			en = getClassLoader().getResources("META-INF/icm/icm-plugins.xml");
		} catch (Throwable t) {
			throw Exceptions.show(t);
		} 
		while (en.hasMoreElements()) {
			final URL url = en.nextElement();
			try (InputStream istream = url.openStream()) {
				final InputSource isource = new InputSource(istream);
				isource.setSystemId(url.toExternalForm());
				parse(isource);
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}

	protected void parse(InputSource pSource) throws SAXException, IOException {
		final ClassLoader cLoader = getClassLoader();
		final Consumer<String> classNameConsumer = new Consumer<String>() {
			@Override
			public void accept(String pClassName) {
				try {
					final Class<?> cl = cLoader.loadClass(pClassName);
					XmlPluginRepository.super.addPluginClass(cl);
				} catch (Throwable t) {
					throw Exceptions.show(t);
				}
			}
		};
		new IcmPluginsParser().parse(pSource, classNameConsumer);
	}
}
