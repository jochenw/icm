package com.github.jochenw.rcm.isclient.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Reflection {
	public static Class<?> requireClass(String pName) {
		return requireClass(Thread.currentThread().getContextClassLoader(), pName);
	}

	public static Class<?> requireClass(ClassLoader pCl, String pName) {
		try {
			return pCl.loadClass(pName);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	public static Method requireMethod(Class<?> pType, String pName, Class<?>... pArgTypes) {
		return requireMethod(pType, pName, false, pArgTypes);
	}

	public static Method requireMethod(Class<?> pType, String pName, boolean pStatic, Class<?>... pArgTypes) {
		try {
			final Method method = pType.getDeclaredMethod(pName, pArgTypes);
			final boolean isStatic = Modifier.isStatic(method.getModifiers());
			if (pStatic  &&  !isStatic) {
				throw new IllegalStateException("Expected method " + method + " to be static.");
			} else if (!pStatic  &&  isStatic) {
				throw new IllegalStateException("Expected method " + method + " not to be static.");
			}
			return method;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
