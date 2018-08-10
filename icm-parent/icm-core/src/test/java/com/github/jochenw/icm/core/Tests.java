package com.github.jochenw.icm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.jochenw.icm.core.api.IcmChangeResource;

public class Tests {
	public static void assertResource(List<IcmChangeResource> pResourceList, String pName, String pUri, Charset pCharset) {
		for (IcmChangeResource r : pResourceList) {
			final int offset = r.getUri().indexOf('!');
			if (offset == -1) {
				if (pName.equals(r.getName())  &&  pUri.equals(r.getUri())) {
					assertEquals(pCharset, r.getCharset());
					return;
				}
			} else {
				final String uri =r.getUri().substring(offset+1);
				if (pName.equals(r.getName())  &&  pUri.equals(uri)) {
					assertEquals(pCharset, r.getCharset());
					return;
				}
			}
		}
		for (IcmChangeResource res : pResourceList) {
			System.out.println("Resource: name=" + res.getName() + ", uri=" + res.getUri());
		}
		fail("No such resource: " + pName + ", " + pUri);
	}

	public static void assertResource(List<IcmChangeResource> pResourceList, String pName, String pUri) {
		assertResource(pResourceList, pName, pUri, StandardCharsets.UTF_8);
	}

}
