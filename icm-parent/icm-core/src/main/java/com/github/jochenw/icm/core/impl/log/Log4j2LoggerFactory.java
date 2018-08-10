package com.github.jochenw.icm.core.impl.log;

import java.io.InputStream;
import java.net.URL;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.util.Exceptions;

public class Log4j2LoggerFactory extends IcmLoggerFactory {
	private LoggerContext loggerContext;

	@Override
	public IcmLogger getLogger(String pId) {
		if (loggerContext == null) {
			final URL url = Thread.currentThread().getContextClassLoader().getResource("log4j2.xml");
			if (url == null) {
				throw new IllegalStateException("Unable to locate log4j2.xml");
			}
			try (InputStream in = url.openStream()) {
				final ConfigurationSource cs = new ConfigurationSource(in, url);
				loggerContext = Configurator.initialize(Thread.currentThread().getContextClassLoader(), cs);
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
		return new Log4j2Logger(loggerContext.getLogger(pId));
	}
}
