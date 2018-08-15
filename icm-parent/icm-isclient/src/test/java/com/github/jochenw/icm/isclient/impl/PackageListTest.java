package com.github.jochenw.icm.isclient.impl;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.github.jochenw.icm.isclient.util.DefaultValuesReader.TypedList;
import com.github.jochenw.icm.isclient.IcmIsClient;


public class PackageListTest {
	@Test
	public void testReadPackageList() throws Exception {
		final IcmIsClient client = Tests.newIcmIsClient();
		final Map<String,Object> input = Collections.emptyMap();
		final Map<String,Object> output = client.invoke("wm.server.packages:packageList", input);
		Assert.assertNotNull(output);
		// {pkgType=1, loadok=0, name=Default, subsystem=false, loadwarning=0, enabled=true, loaderr=0}
		@SuppressWarnings("unchecked")
		final TypedList<Object> packages = (TypedList<Object>) output.get("packages");
		Assert.assertNotNull(packages);
		Assert.assertEquals("record", packages.getType());
		boolean wmPublicFound = false;
		for (Object pkg : packages) {
			@SuppressWarnings("unchecked")
			final Map<String,Object> pkgMap = (Map<String,Object>) pkg;
			if (Tests.isTrue(pkgMap.get("enabled"))  &&  "WmPublic".equals(pkgMap.get("name")) ) {
				wmPublicFound = true;
				break;
			}
		}
		Assert.assertTrue(wmPublicFound);
	}
}
