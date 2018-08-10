package com.github.jochenw.icm.core.impl.log;

import org.apache.logging.log4j.core.Logger;

import com.github.jochenw.icm.core.api.log.IcmLogger;

public class Log4j2Logger extends IcmLogger {
	private final Logger log;

	public Log4j2Logger(Logger pLogger) {
		log = pLogger;
	}

	@Override
	public void log(Level pLevel, String pMessage) {
		switch (pLevel) {
		case TRACE: log.trace(pMessage); break;
		case DEBUG: log.debug(pMessage); break;
		case INFO: log.info(pMessage); break;
		case WARN: log.warn(pMessage); break;
		case ERROR: log.error(pMessage); break;
		case FATAL: log.fatal(pMessage); break;
		default:
			throw new IllegalStateException("Invalid level: " + pLevel);
		}
	}

	@Override
	public void log(Level pLevel, String pMessage, Throwable pTh) {
		switch (pLevel) {
		case ERROR: log.error(pMessage, pTh); break;
		case FATAL: log.fatal(pMessage, pTh); break;
		default:
			throw new IllegalStateException("Invalid level: " + pLevel);
		}
	}

	@Override
	public boolean isEnabledFor(Level pLevel) {
		switch (pLevel) {
		case TRACE: return log.isTraceEnabled();
		case DEBUG: return log.isDebugEnabled();
		case INFO: return log.isInfoEnabled();
		case WARN: return log.isWarnEnabled();
		case ERROR: return log.isErrorEnabled();
		case FATAL: return log.isFatalEnabled();
		case ALL: return log.isTraceEnabled();
		case NONE: return !log.isFatalEnabled();
		default:
			throw new IllegalStateException("Invalid level: " + pLevel);
		}
	}

}
