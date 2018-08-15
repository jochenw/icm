package com.github.jochenw.icm.isclient.svc;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import com.softwareag.util.IDataMap;
import com.wm.app.b2b.server.ServiceManager;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;
import com.wm.lang.ns.NSName;


@SuppressWarnings("deprecation")
public abstract class AbstractIsService {
	public abstract Map<String,Object> run(Map<String,Object> pInput);
	public IData run(IData pInput) {
		final Map<String,Object> input = new IDataMap(pInput);
		final Map<String,Object> output = run(input);
		return asIData(output);
	}

	public void invoke(IData pPipeline) {
		final IData output = run(pPipeline);
		final IDataCursor outputCrsr = output.getCursor();
		if (outputCrsr.first()) {
			final IDataCursor pipelineCrsr = pPipeline.getCursor();
			do {
				final String key = outputCrsr.getKey();
				final Object value = outputCrsr.getValue();
				IDataUtil.put(pipelineCrsr, key, value);
			} while (outputCrsr.next());
			pipelineCrsr.destroy();
		}
		outputCrsr.destroy();
	}

	protected IData asIData(Map<String,Object> pMap) {
		final IData data = IDataFactory.create();
		final IDataCursor crsr = data.getCursor();
		for (Map.Entry<String, Object> en : pMap.entrySet()) {
			final String key = en.getKey();
			final Object value = en.getValue();
			IDataUtil.put(crsr, key, value);
		}
		crsr.destroy();
		return data;
	}

	protected Map<String,Object> asMap(Object... pValues) {
		final Map<String,Object> map = new HashMap<>();
		if (pValues != null) {
			for (int i = 0;  i < pValues.length;  i += 2) {
				final String key = (String) pValues[i];
				final Object value = pValues[i+1];
				map.put(key, value);
			}
		}
		return map;
	}

	protected IData asIData(Object... pValues) {
		final IData data = IDataFactory.create();
		if (pValues != null) {
			final IDataCursor crsr = data.getCursor();
			for (int i = 0;  i < pValues.length;  i += 2) {
				final String key = (String) pValues[i];
				final Object value = pValues[i+1];
				IDataUtil.put(crsr, key, value);
			}
			crsr.destroy();
		}
		return data;
	}

	protected Object getValue(Map<String,Object> pMap, String pKey) {
		return pMap.get(pKey);
	}

	protected String getString(Map<String,Object> pMap, String pKey) {
		final Object v = getValue(pMap, pKey);
		if (v == null) {
			return null;
		} else if (v instanceof String) {
			return (String) v;
		} else {
			throw new IllegalStateException("Expected string for key " + pKey + ", got " + v.getClass());
		}
	}

	protected String requireString(Map<String,Object> pMap, String pKey) {
		final String s = getString(pMap, pKey);
		if (s == null  ||  s.length() == 0) {
			throw new IllegalStateException("Missing, or empty, value for key " + pKey);
		}
		return s;
	}

	protected Map<String,Object> invoke(String pService, Object... pParameters) {
		final IData input = asIData(pParameters);
		return invoke(pService, input);
	}

	protected Map<String,Object> invoke(String pService, IData pParameters) {
		try {
			final NSName nsName = NSName.create(pService);
			@SuppressWarnings("deprecation")
			final IData output = ServiceManager.invoke(nsName, pParameters, false);
			return asMap(output);
		} catch (Exception e) {
			throw new UndeclaredThrowableException(e);
		}
	}
}
