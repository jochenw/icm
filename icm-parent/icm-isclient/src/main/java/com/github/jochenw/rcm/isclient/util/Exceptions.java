package com.github.jochenw.rcm.isclient.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

/**
 * Utility class for working with Exceptions. Provides static utility methods.
 */
public class Exceptions {
	public static interface ThrowingRunnable {
		public void run() throws Throwable;
	}

	/**
     * Private constructor, to prevent accidental instantiation.
     */
    private Exceptions() {
        // Does nothing.
    }

    /**
     * Throws the given {@link Throwable}, or a suitable subclass, which can be thrown
     * safely, without affecting the calling methods signature.
     */
    public static RuntimeException show(Throwable pTh) {
        if (pTh == null) {
            throw new NullPointerException("The Throwable must not be null.");
        } else if (pTh instanceof RuntimeException) {
            throw (RuntimeException) pTh;
        } else if (pTh instanceof Error) {
            throw (Error) pTh;
        } else if (pTh instanceof IOException) {
            throw newUncheckedIOException((IOException) pTh);
        } else {
            throw newUncheckedException(pTh);
        }
    }


    /**
     * Throws the given {@link Throwable}, or a suitable subclass, which can be thrown
     * safely, without affecting the calling methods signature.
     */
    public static <T extends Throwable> RuntimeException show(Throwable pTh, Class<T> pClass) throws T {
        if (pTh == null) {
            throw new NullPointerException("The Throwable must not be null.");
        } else if (pClass.isAssignableFrom(pTh.getClass())) {
            throw pClass.cast(pTh);
        } else if (pTh instanceof RuntimeException) {
            throw (RuntimeException) pTh;
        } else if (pTh instanceof Error) {
            throw (Error) pTh;
        } else if (pTh instanceof IOException) {
            throw newUncheckedIOException((IOException) pTh);
        } else {
            throw newUncheckedException(pTh);
        }
    }

    /** Converts the given {@link IOException} into a {@link RuntimeException}.
     */
    public static RuntimeException newUncheckedIOException(IOException pExc) {
        return new UncheckedIOException(pExc);
    }

    /** Converts the given {@link Throwable} into a {@link RuntimeException}.
     */
    public static RuntimeException newUncheckedException(Throwable pTh) {
        return new UndeclaredThrowableException(pTh);
    }

    /**
     * Throws the given {@link Throwable}, or a suitable subclass, which can be thrown
     * safely, without affecting the calling methods signature (the exception being the classes
     * T1, and T2.)
     * @param pTh The throwable to convert.
     * @param pClass1 An exception class, which may be thrown by the method, affecting the signature of the calling method.
     * @param pClass2 Amn exception class Another exception, which may be thrown by the method, affecting the signature of the calling method.
     */
    public static <T1 extends Throwable, T2 extends Throwable> RuntimeException show(Throwable pTh, Class<T1> pClass1, Class<T2> pClass2) throws T1, T2 {
        if (pTh == null) {
            throw new NullPointerException("The Throwable must not be null.");
        } else if (pClass1.isAssignableFrom(pTh.getClass())) {
            throw pClass1.cast(pTh);
        } else if (pClass2.isAssignableFrom(pTh.getClass())) {
            throw pClass2.cast(pTh);
        } else if (pTh instanceof RuntimeException) {
            throw (RuntimeException) pTh;
        } else if (pTh instanceof Error) {
            throw (Error) pTh;
        } else if (pTh instanceof IOException) {
            throw newUncheckedIOException((IOException) pTh);
        } else {
            throw newUncheckedException(pTh);
        }
    }

    /** Returns the given throwables stacktrace as a string.
     * @param The {@link Throwable}, for which to generate a stacktrace.
     * @return A string with the stack trace of the given {@link Throwable}.
     */
    public static String toString(Throwable pTh) {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);
    	pTh.printStackTrace(pw);
    	pw.close();
    	return sw.toString();
    }

    /** Invokes the given {@link ThrowingRunnable}. If an exception is thrown, the exception is catched, and converted into a
     * {@link RuntimeException}.
     * @param The {@link ThrowingRunnable} to execute.
     */
    public static void run(ThrowingRunnable pRunnable) {
    	try {
    		pRunnable.run();
    	} catch (Throwable t) {
    		throw show(t);
    	}
    }

    /** Invokes the given {@link Runnable}. If an exception is thrown, the exception is catched, and converted into a
     * {@link RuntimeException}.
     * @param The {@link Runnable} to execute.
     */
    public static void run(Runnable pRunnable) {
    	try {
    		pRunnable.run();
    	} catch (Throwable t) {
    		throw show(t);
    	}
    }

    public static Throwable close(AutoCloseable pCloseable, Throwable pTh) {
    	Objects.requireNonNull(pCloseable, "Closeable");
    	try {
    		pCloseable.close();
    	} catch (Throwable t) {
    		if (pTh == null) {
    			return t;
    		}
    	}
    	return pTh;
    }
}
