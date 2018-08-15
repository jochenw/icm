package com.github.jochenw.icm.isclient;

import java.util.Map;

public interface ServiceInvoker {
	public default Map<String,Object> invoke(String pNamespace, String pService, Map<String,Object> pInput) {
		return invoke(pNamespace + ":" + pService, pInput);
	}

	public abstract Map<String,Object> invoke(String pFullyQualifiedService, Map<String,Object> pInput);
}
