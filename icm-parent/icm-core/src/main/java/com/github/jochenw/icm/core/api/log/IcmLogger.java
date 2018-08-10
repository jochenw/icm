package com.github.jochenw.icm.core.api.log;

public abstract class IcmLogger {
	public enum Level {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL, ALL, NONE;
	}
	protected abstract void log(Level pLevel, String pMessage);
	protected abstract void log(Level pLevel, String pMessage, Throwable pTh);
	public abstract boolean isEnabledFor(Level pLevel);
	public void trace(String pMessage) {
		if (isEnabledFor(Level.TRACE)) {
			log(Level.TRACE, pMessage);
		}
	}
	public void trace(String pMessage, Object... pArgs) {
		if (isEnabledFor(Level.TRACE)) {
			log(Level.TRACE, format(pMessage, pArgs));
		}
	}
	public void debug(String pMessage) {
		if (isEnabledFor(Level.DEBUG)) {
			log(Level.DEBUG, pMessage);
		}
	}
	public void debug(String pMessage, Object... pArgs) {
		if (isEnabledFor(Level.DEBUG)) {
			log(Level.DEBUG, format(pMessage, pArgs));
		}
	}
	public void info(String pMessage) {
		if (isEnabledFor(Level.INFO)) {
			log(Level.INFO, pMessage);
		}
	}
	public void info(String pMessage, Object... pArgs) {
		if (isEnabledFor(Level.INFO)) {
			log(Level.INFO, format(pMessage, pArgs));
		}
	}
	public void warn(String pMessage) {
		if (isEnabledFor(Level.WARN)) {
			log(Level.WARN, pMessage);
		}
	}
	public void warn(String pMessage, Object... pArgs) {
		if (isEnabledFor(Level.WARN)) {
			log(Level.WARN, format(pMessage, pArgs));
		}
	}
	public void warn(Throwable pTh) {
		if (isEnabledFor(Level.WARN)) {
			log(Level.WARN, pTh.getMessage(), pTh);
		}
	}
	public void error(String pMessage) {
		if (isEnabledFor(Level.ERROR)) {
			log(Level.ERROR, pMessage);
		}
	}
	public void error(String pMessage, Object... pArgs) {
		if (isEnabledFor(Level.ERROR)) {
			log(Level.ERROR, format(pMessage, pArgs));
		}
	}
	public void error(String pMessage, Throwable pTh) {
		if (isEnabledFor(Level.ERROR)) {
			log(Level.ERROR, pMessage, pTh);
		}
	}
	public void error(Throwable pTh) {
		if (isEnabledFor(Level.ERROR)) {
			log(Level.ERROR, pTh.getMessage(), pTh);
		}
	}
	public void fatal(String pMessage) {
		if (isEnabledFor(Level.FATAL)) {
			log(Level.FATAL, pMessage);
		}
	}
	public void fatal(String pMessage, Object... pArgs) {
		if (isEnabledFor(Level.FATAL)) {
			log(Level.FATAL, format(pMessage, pArgs));
		}
	}
	public void fatal(String pMessage, Throwable pTh) {
		if (isEnabledFor(Level.FATAL)) {
			log(Level.FATAL, pMessage, pTh);
		}
	}
	public void fatal(Throwable pTh) {
		if (isEnabledFor(Level.FATAL)) {
			log(Level.FATAL, pTh.getMessage(), pTh);
		}
	}


	protected String format(String pFormat, Object[] pArgs) {
		final StringBuilder sb = new StringBuilder();
		sb.append(pFormat);
		if (pArgs != null) {
			for (int i = 0;  i < pArgs.length;  i++) {
				sb.append(", ");
				format(sb, pArgs[i]);
			}
		}
		return sb.toString();
	}

	protected void format(StringBuilder pSb, Object pValue) {
		pSb.append(pValue);
	}
}
