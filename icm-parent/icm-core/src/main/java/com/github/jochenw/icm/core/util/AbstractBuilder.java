package com.github.jochenw.icm.core.util;

public abstract class AbstractBuilder {
	private boolean immutable;
	private Object instance;

	protected void assertMutable() {
		if (immutable) {
			throw new IllegalStateException("This object is no longer mutable.");
		}
	}
	protected void makeImmutable() {
		immutable = true;
	}

	public boolean isMutable() {
		return !immutable;
	}

	public Object build() {
		if (instance == null) {
			instance = newInstance();
			makeImmutable();
		}
		return instance;
	}

	protected Object getInstance() {
		return instance;
	}
	protected abstract Object newInstance();
}
