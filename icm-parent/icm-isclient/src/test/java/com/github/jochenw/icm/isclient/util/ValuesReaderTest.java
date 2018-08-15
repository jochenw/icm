package com.github.jochenw.icm.isclient.util;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Map;

import org.junit.Test;

import com.github.jochenw.icm.isclient.util.DefaultValuesReader;
import com.github.jochenw.icm.isclient.util.DefaultValuesReader.TypedList;
import com.github.jochenw.icm.isclient.util.DefaultValuesReader.TypedMap;


public class ValuesReaderTest {
	// Test reading PackageList.xml
	@Test
	public void testReadPackageListXml() {
		final Map<String, Object> map = read("PackageList.xml");
		assertEquals(2, map.size());
		final String xmlVersion= (String) map.get("<?xml version");
		assertEquals("'1.0' encoding='UTF-8'?><Values version='2.0'", xmlVersion.trim());
		@SuppressWarnings("unchecked")
		final TypedList<Object> packages = (TypedList<Object>) map.get("packages");
		assertEquals("record", packages.getType());
		assertEquals(24, packages.size());
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> package0 = (TypedMap<String,Object>) packages.get(0);
			assertEquals("wm.server.packages$1", package0.getJavaClass());
			assertEquals(7, package0.size());
			assertEquals("Default", package0.get("name"));
			assertEquals("true", package0.get("enabled"));
			assertEquals("0", package0.get("loadok"));
			assertEquals("1", package0.get("pkgType"));
			assertEquals("0", package0.get("loaderr"));
			assertEquals("0", package0.get("loadwarning"));
			assertEquals(Boolean.FALSE, package0.get("subsystem"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> package12 = (TypedMap<String,Object>) packages.get(12);
			assertEquals("wm.server.packages$1", package12.getJavaClass());
			assertEquals("WmISExtDC", package12.get("name"));
			assertEquals("true", package12.get("enabled"));
			assertEquals("12", package12.get("loadok"));
			assertEquals("1", package12.get("pkgType"));
			assertEquals("0", package12.get("loaderr"));
			assertEquals("0", package12.get("loadwarning"));
			assertEquals(Boolean.FALSE, package12.get("subsystem"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> package23 = (TypedMap<String,Object>) packages.get(23);
			assertEquals("wm.server.packages$1", package23.getJavaClass());
			assertEquals("WmXSLT", package23.get("name"));
			assertEquals("true", package23.get("enabled"));
			assertEquals("15", package23.get("loadok"));
			assertEquals("1", package23.get("pkgType"));
			assertEquals("0", package23.get("loaderr"));
			assertEquals("0", package23.get("loadwarning"));
			assertEquals(Boolean.FALSE, package23.get("subsystem"));
		}
	}

	private Map<String, Object> read(String pResource) {
		final URL url = getClass().getResource(pResource);
		assertNotNull(url);
		final Map<String,Object> map = new DefaultValuesReader().read(url);
		assertNotNull(map);
		return map;
	}

	@Test
	public void testReadCacheManagerListXml() throws Exception {
		final Map<String,Object> map = read("CacheManagerList.xml");
		assertEquals(3, map.size());
		assertEquals("'1.0' encoding='UTF-8'?><Values version='2.0'", ((String) map.get("<?xml version")).trim());
		@SuppressWarnings("unchecked")
		final TypedList<Object> publicList = (TypedList<Object>) map.get("publicCacheManagers");
		assertEquals(1, publicList.getDepth());
		assertEquals("record", publicList.getType());
		assertEquals(0, publicList.size());
		@SuppressWarnings("unchecked")
		final TypedList<Object> systemList = (TypedList<Object>) map.get("systemCacheManagers");
		assertEquals("record", systemList.getType());
		assertEquals(1, systemList.getDepth());
		assertEquals(5, systemList.size());

		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> cm0 = (TypedMap<String,Object>) systemList.get(0);
			assertEquals("com.wm.util.Values", cm0.getJavaClass());
			assertEquals("SoftwareAG.IS.ART", cm0.get("cacheManagerName"));
			assertEquals("./cacheStore/SoftwareAG-IS-ART", cm0.get("diskStorePath"));
			assertEquals("STATUS_ALIVE", cm0.get("status"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> cm1 = (TypedMap<String,Object>) systemList.get(1);
			assertEquals("com.wm.util.Values", cm1.getJavaClass());
			assertEquals("SoftwareAG.IS.Core", cm1.get("cacheManagerName"));
			assertEquals("./cacheStore/SoftwareAG-IS-Core", cm1.get("diskStorePath"));
			assertEquals("STATUS_ALIVE", cm1.get("status"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> cm2 = (TypedMap<String,Object>) systemList.get(2);
			assertEquals("com.wm.util.Values", cm2.getJavaClass());
			assertEquals("SoftwareAG.IS.PE", cm2.get("cacheManagerName"));
			assertEquals("./cacheStore/SoftwareAG-IS-PE", cm2.get("diskStorePath"));
			assertEquals("STATUS_ALIVE", cm2.get("status"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> cm3 = (TypedMap<String,Object>) systemList.get(3);
			assertEquals("com.wm.util.Values", cm3.getJavaClass());
			assertEquals("SoftwareAG.IS.Services", cm3.get("cacheManagerName"));
			assertEquals("./cacheStore/SoftwareAG-IS-Services", cm3.get("diskStorePath"));
			assertEquals("STATUS_ALIVE", cm3.get("status"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> cm4 = (TypedMap<String,Object>) systemList.get(4);
			assertEquals("com.wm.util.Values", cm4.getJavaClass());
			assertEquals("SoftwareAG.IS.WMN", cm4.get("cacheManagerName"));
			assertEquals("./cacheStore/SoftwareAG-IS-WMN", cm4.get("diskStorePath"));
			assertEquals("STATUS_ALIVE", cm4.get("status"));
		}
	}

	@Test
	public void testReadCacheListXml() {
		final Map<String,Object> map = read("CacheList.xml");
		assertEquals(3, map.size());
		assertEquals("SoftwareAG.IS.ART", map.get("cacheManagerName"));
		assertEquals("'1.0' encoding='UTF-8'?><Values version='2.0'", ((String) map.get("<?xml version")).trim());

		@SuppressWarnings("unchecked")
		final TypedList<Object> caches = (TypedList<Object>) map.get("caches");
		assertEquals("record", caches.getType());
		assertEquals(2, caches.size());

		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> c0 = (TypedMap<String,Object>) caches.get(0);
			assertEquals(4, c0.size());
			assertEquals("com.wm.util.Values", c0.getJavaClass());
			assertEquals("ListenerDataStore", c0.get("cacheName"));
			assertEquals("true", c0.get("enabled"));
			assertEquals("true", c0.get("isInternal"));
			assertEquals("", c0.get("maxMemoryOffHeap"));
		}
		{
			@SuppressWarnings("unchecked")
			final TypedMap<String,Object> c1 = (TypedMap<String,Object>) caches.get(1);
			assertEquals(4, c1.size());
			assertEquals("com.wm.util.Values", c1.getJavaClass());
			assertEquals("ListenerStateStore", c1.get("cacheName"));
			assertEquals("true", c1.get("enabled"));
			assertEquals("true", c1.get("isInternal"));
			assertEquals("", c1.get("maxMemoryOffHeap"));
		}
	}

	@Test
	public void testReadScheduledTasksListXml() {
		final Map<String,Object> map = read("ScheduledTasksList.xml");
		assertEquals(3, map.size());
		assertEquals("'1.0' encoding='UTF-8'?><Values version='2.0'", ((String) map.get("<?xml version")).trim());

		@SuppressWarnings("unchecked")
		final TypedList<Object> tasks = (TypedList<Object>) map.get("tasks");
		assertEquals(1, tasks.size());
		assertEquals(1, tasks.getDepth());
		assertEquals("record", tasks.getType());

		@SuppressWarnings("unchecked")
		final TypedMap<String,Object> task0 = (TypedMap<String,Object>) tasks.get(0);
		assertEquals(21, task0.size());
		assertEquals("repeat", task0.get("type"));
		assertEquals("wm.monitor.admin:imageCleanup", task0.get("name"));
		assertEquals("wm.monitor.admin:imageCleanup", task0.get("service"));
		assertEquals("MCJWI01.eur.ad.sag", task0.get("target"));
		assertEquals("0", task0.get("lateness"));
		assertEquals("0", task0.get("latenessAction"));
		assertEquals("1506114321000", task0.get("nextRun"));
		assertEquals("131565", task0.get("msDelta"));
		assertEquals("300000", task0.get("interval"));
		assertEquals("3ffff920-8411-11e7-a04b-ffffffffec8d", task0.get("oid"));
		assertEquals("ready", task0.get("execState"));
		assertEquals("unexpired", task0.get("schedState"));
		assertEquals("true", task0.get("doNotOverlap"));
		assertEquals("1503059528114", task0.get("startDate"));
		assertEquals("-1", task0.get("endDate"));
		assertEquals("0", task0.get("minuteMask"));
		assertEquals("0", task0.get("hourMask"));
		assertEquals("0", task0.get("dayOfMonthMask"));
		assertEquals("0", task0.get("monthMask"));
		assertEquals("0", task0.get("dayOfWeekMask"));
		assertEquals("Administrator", task0.get("runAsUser"));

		@SuppressWarnings("unchecked")
		final TypedList<Object> extTasks = (TypedList<Object>) map.get("extTasks");
		assertEquals(0, extTasks.size());
	}

	@Test
	public void testReadDataTypesXml() {
		final Map<String,Object> map = read("DataTypes.xml");
		assertEquals(10, map.size());
		assertEquals(Boolean.FALSE, map.get("boolean"));
		assertEquals(Integer.valueOf(1), map.get("integer"));
		assertEquals(Long.valueOf(1), map.get("long"));
		assertEquals(Short.valueOf((short) 1), map.get("short"));
		assertEquals(Byte.valueOf((byte) 1), map.get("byte"));
		assertEquals(Double.valueOf(1.0d), map.get("double"));
		assertEquals(Float.valueOf(1.0f), map.get("float"));

		@SuppressWarnings("unchecked")
		final TypedList<Object> stringList = (TypedList<Object>) map.get("stringList");
		assertEquals(2, stringList.size());
		assertEquals("value", stringList.getType());
		assertEquals("foo", stringList.get(0));
		assertEquals("bar", stringList.get(1));

		@SuppressWarnings("unchecked")
		final TypedList<Object> documentList = (TypedList<Object>) map.get("documentList");
		assertEquals(0, documentList.size());
		assertEquals("record", documentList.getType());

		@SuppressWarnings("unchecked")
		final TypedList<Object> objectList = (TypedList<Object>) map.get("objectList");
		assertEquals(2, objectList.size());
		assertEquals("value", objectList.getType());
		assertEquals("foo", objectList.get(0));
		assertEquals("bar", objectList.get(1));
	}
}
