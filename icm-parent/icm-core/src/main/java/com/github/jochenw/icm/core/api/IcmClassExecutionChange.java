package com.github.jochenw.icm.core.api;

import com.github.jochenw.icm.core.api.IcmChangeInstaller.Context;

public interface IcmClassExecutionChange {
	void run(Context pContext);
}
