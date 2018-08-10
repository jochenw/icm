package com.github.jochenw.rcm.isclient.util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataUtil;


public class Data {
	private static final Class<?> iDataClass = Reflection.requireClass("com.wm.data.IData");
	private static final Class<?> iDataCursorClass = Reflection.requireClass("com.wm.data.IDataCursor");
	private static final Class<?> iDataFactoryClass = Reflection.requireClass("com.wm.data.IDataFactory");
	private static final Class<?> iDataUtilClass = Reflection.requireClass("com.wm.data.IDataUtil");
	private static final Method iDataFactoryCreateMethod = Reflection.requireMethod(iDataFactoryClass, "create", true);
	private static final Method iDataGetCursorMethod = Reflection.requireMethod(iDataClass, "getCursor");
	private static final Method iDataUtilPutMethod = Reflection.requireMethod(iDataUtilClass, "put", true, iDataCursorClass, String.class, Object.class);
	private static final Method iDataCursorDestroyMethod = Reflection.requireMethod(iDataCursorClass, "destroy");
	private static final Method iDataCursorFirstMethod = Reflection.requireMethod(iDataCursorClass, "first");
	private static final Method iDataCursorNextMethod = Reflection.requireMethod(iDataCursorClass, "next");
	private static final Method iDataCursorGetKeyMethod = Reflection.requireMethod(iDataCursorClass, "getKey");
	private static final Method iDataCursorGetValueMethod = Reflection.requireMethod(iDataCursorClass, "getValue");

	public static <O> O asIData(Map<String,Object> pMap) {
		if (pMap == null) {
			return null;
		}
		try {
			final Object data = iDataFactoryCreateMethod.invoke(null);
			final Object crsr = iDataGetCursorMethod.invoke(data);
			for (Map.Entry<String,Object> en : pMap.entrySet()) {
				iDataUtilPutMethod.invoke(null, crsr, en.getKey(), en.getValue());
			}
			iDataCursorDestroyMethod.invoke(crsr);
			@SuppressWarnings("unchecked")
			final O o = (O) data;
			return o;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	public static Map<String, Object> toMap(Object pData) {
		if (pData == null) {
			return null;
		}
		try {
			final Map<String,Object> map = new HashMap<>();
			final Object crsr = iDataGetCursorMethod.invoke(pData);
			final Boolean hasFirst = (Boolean) iDataCursorFirstMethod.invoke(crsr);
			if (hasFirst.booleanValue()) {
				Boolean hasNext;
				do {
					map.put((String) iDataCursorGetKeyMethod.invoke(crsr),
							iDataCursorGetValueMethod.invoke(crsr));
					hasNext = (Boolean) iDataCursorNextMethod.invoke(crsr);
				} while (hasNext.booleanValue());
			}
			iDataCursorDestroyMethod.invoke(crsr);
			return map;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	public static Map<String,Object> asMap(Object... pArgs) {
		final Map<String,Object> map = new HashMap<>();
		if (pArgs != null) {
			for (int i = 0;  i < pArgs.length;  i += 2) {
				map.put((String) pArgs[i], pArgs[i+1]);
			}
		}
		return map;
	}

	public static void mergeTo(IData pPipeline, Map<String,Object> pOutput) {
		final IDataCursor crsr = pPipeline.getCursor();
		for (Map.Entry<String,Object> en : pOutput.entrySet()) {
			final String key = en.getKey();
			final Object value = en.getValue();
			IDataUtil.put(crsr, key, value);
		}
	}

	public static String getStringValue(Map<String, Object> pMap, String pKey) {
		final Object v = pMap.get(pKey);
		if (v == null) {
			return null;
		}
		if (v instanceof String) {
			throw new IllegalArgumentException("Expected string for parameter " + pKey + ", got " + v.getClass().getName());
		}
		return (String) v;
	}

	public static File getFileValue(Map<String, Object> pMap, String pKey) {
		final Object v = pMap.get(pKey);
		if (v == null) {
			return null;
		} else if (v instanceof File) {
			return (File) v;
		} else if (v instanceof String) {
			final String s = (String) v;
			if (s.length() == 0) {
				throw new IllegalArgumentException("Expected file name for parameter " + pKey + ", got empty string.");
			}
			return new File(s);
		} else {
			throw new IllegalArgumentException("Expected string, or file for parameter " + pKey + ", got " + v.getClass().getName());
		}
	}

	public static File requireFileValue(Map<String,Object> pMap, String pKey) {
		final File f = getFileValue(pMap, pKey);
		if (f == null) {
			throw new NullPointerException("Missing, or empty parameter (expected file name): " + pKey);
		}
		return f;
	}

	public static String[] getStringArrayValue(Map<String,Object> pMap, String pKey) {
		final Object o = pMap.get(pKey);
		if (o == null) {
			return null;
		} else {
			final List<String> list = new ArrayList<>();
			final List<Object> olist;
			if (o instanceof Object[]) {
				olist = Arrays.asList((Object[]) o);
			} else if (o instanceof List) {
				@SuppressWarnings("unchecked")
				final List<Object> l = (List<Object>) o;
				olist = l;
			} else {
				throw new IllegalArgumentException("Expected string array, or string list for parameter: " + pKey
						                            + ", got " + o.getClass().getName());
			}
			for (Object v : olist) {
				if (v == null) {
					throw new IllegalArgumentException("A string array element must not be null.");
				}
				if (v instanceof String) {
					list.add((String) v);
				} else {
					throw new IllegalArgumentException("Invalid type for a string array element: " + v.getClass().getName());
				}
			}
			return list.toArray(new String[list.size()]);
		}
	}
}
