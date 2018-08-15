package com.github.jochenw.icm.isclient.svc.admin.jdbc;

import java.util.Map;

import com.github.jochenw.icm.isclient.svc.AbstractIsService;
import com.wm.data.IData;

public class AddDriverAliasSvc extends AbstractIsService {
	@Override
	public Map<String, Object> run(Map<String, Object> pInput) {
		final String driver = requireString(pInput, "driver");
		final String description = getString(pInput, "description");
		final String className = requireString(pInput, "className");
		final Map<String,Object> output = invoke("wm.server.jdbcpool:addDriverAlias", "driver", driver, "description", description, "classname", className);
		return output;
	}
}
