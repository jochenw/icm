package com.github.jochenw.icm.core.impl.log;

import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.github.jochenw.icm.core.api.log.IcmLogger;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.api.log.IcmLogger.Level;

public class SimpleLoggerFactory extends IcmLoggerFactory {
	private Appendable appendable;
	private int logLevel;
	private final long startTime;

	public SimpleLoggerFactory(Appendable pAppendable, Level pLogLevel) {
		appendable = pAppendable;
		startTime = System.currentTimeMillis();
		switch (pLogLevel) {
		case NONE: logLevel = 6; break;
		case TRACE: logLevel = 0; break;
		case DEBUG: logLevel = 1; break;
		case INFO: logLevel = 2; break;
		case WARN: logLevel = 3; break;
		case ERROR: logLevel = 4; break;
		case FATAL: logLevel = 5; break;
		case ALL: logLevel = 6; break;
		default:
			throw new IllegalStateException("Invalid level: " + pLogLevel);
		}
	}
	
	void log(Level pLevel, String pId, String pMessage) throws IOException {
		synchronized (appendable) {
			if (appendable != null) {
				appendable.append(String.valueOf(System.currentTimeMillis()-startTime));
			}
			appendable.append(' ');
			appendable.append(pLevel.name());
			appendable.append(' ');
			appendable.append(pId);
			appendable.append(": ");
			appendable.append(pMessage);
			appendable.append('\n');
			if (appendable instanceof Flushable) {
				((Flushable) appendable).flush();
			}
		}
	}

	void log(Level pLevel, String pId, String pMessage, Throwable pTh) throws IOException {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		pTh.printStackTrace(pw);
		pw.close();
		synchronized(appendable) {
			final String s = sw.toString();
			if (appendable != null) {
				appendable.append(String.valueOf(System.currentTimeMillis()-startTime));
			}
			appendable.append(' ');
			appendable.append(pLevel.name());
			appendable.append(' ');
			appendable.append(pId);
			appendable.append(": ");
			appendable.append(pMessage);
			appendable.append('\n');
			appendable.append(s);
			if (appendable instanceof Flushable) {
				((Flushable) appendable).flush();
			}
		}
	}

	@Override
	public IcmLogger getLogger(String pId) {
		return new SimpleLogger(this, pId);
	}

	public boolean isEnabledFor(Level pLevel) {
		final int level;
		switch (pLevel) {
		case NONE: level = -1; break;
		case TRACE: level = 0; break;
		case DEBUG: level = 1; break;
		case INFO: level = 2; break;
		case WARN: level = 3; break;
		case ERROR: level = 4; break;
		case FATAL: level = 5; break;
		case ALL: level = 6; break;
		default:
			throw new IllegalStateException("Invalid level: " + pLevel);
		}
		return logLevel <= level;
	}

}
