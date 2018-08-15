package com.github.jochenw.icm.isclient;

import java.lang.reflect.Method;
import java.util.Map;

import com.github.jochenw.icm.isclient.util.Data;
import com.github.jochenw.icm.isclient.util.Exceptions;
import com.github.jochenw.icm.isclient.util.Reflection;

public class IsInternalServiceInvoker implements ServiceInvoker {
	private static final Class<?> serviceClass = Reflection.requireClass("com.wm.app.b2b.server.Service");
	private static final Class<?> nsNameClass = Reflection.requireClass("com.wm.lang.ns.NSName");
	private static final Class<?> iDataClass = Reflection.requireClass("com.wm.data.IData");
	private static final Method nsNameCreateMethod = Reflection.requireMethod(nsNameClass, "create", true, String.class);
	private static final Method serviceDoInvokeMethod = Reflection.requireMethod(serviceClass, "doInvoke", true, nsNameClass, iDataClass);

	@Override
	public Map<String, Object> invoke(String pFullyQualifiedService, Map<String, Object> pInput) {
		try {
			final Object input = Data.asIData(pInput);
			final Object name = nsNameCreateMethod.invoke(null, pFullyQualifiedService);
			final Object output = serviceDoInvokeMethod.invoke(null, name, input);
			final Map<String,Object> map = (Map<String,Object>) Data.toMap(output);
			return map;
		} catch (Exception e) {
			throw Exceptions.show(e);
		}
	}
}
