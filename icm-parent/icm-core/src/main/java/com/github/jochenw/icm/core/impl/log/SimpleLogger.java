package com.github.jochenw.icm.core.impl.log;

import java.io.IOException;

import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.util.Exceptions;

public class SimpleLogger extends IcmLogger {
	private final String id;
	private final SimpleLoggerFactory factory;

	public SimpleLogger(SimpleLoggerFactory pFactory, String pId) {
		factory = pFactory;
		id = pId;
	}

	@Override
	protected void log(Level pLevel, String pMessage) {
		try {
			factory.log(pLevel, id, pMessage);
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}

	@Override
	protected void log(Level pLevel, String pMessage, Throwable pTh) {
		try {
			factory.log(pLevel, id, pMessage, pTh);
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}

	@Override
	public boolean isEnabledFor(Level pLevel) {
		return factory.isEnabledFor(pLevel);
	}

}
