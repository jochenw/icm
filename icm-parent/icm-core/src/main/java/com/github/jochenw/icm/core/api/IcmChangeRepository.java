package com.github.jochenw.icm.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface IcmChangeRepository {
	public void list(Consumer<IcmChangeResource> pConsumer);
	InputStream open(IcmChangeResource pResource) throws IOException;
}
