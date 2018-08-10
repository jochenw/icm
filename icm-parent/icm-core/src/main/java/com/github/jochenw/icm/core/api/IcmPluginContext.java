package com.github.jochenw.icm.core.api;

import java.util.List;

import com.github.jochenw.icm.core.api.plugins.LifeCycleAware;

public interface IcmPluginContext {
	public interface Committable {
		void commit();
		void rollback();
	}
	void add(Committable pCommittable);
	List<Committable> getTransactions();
	<C extends LifeCycleAware> C getContextFor(String pId);
}
