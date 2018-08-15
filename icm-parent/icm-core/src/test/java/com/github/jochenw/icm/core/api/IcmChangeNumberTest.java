package com.github.jochenw.icm.core.api;

import static org.junit.Assert.*;

import java.util.Comparator;

import org.junit.Test;

import com.github.jochenw.icm.core.api.DefaultIcmChangeNumberHandler;
import com.github.jochenw.icm.core.api.IcmChangeNumber;

public class IcmChangeNumberTest {
	@Test
	public void testParseOkay() {
		{
			final IcmChangeNumber v = IcmChangeNumber.valueOf("0.2.13");
			assertNotNull(v);
			final int[] num = v.getNumbers();
			assertNotNull(num);
			assertEquals(3, num.length);
			assertEquals(0, num[0]);
			assertEquals(2, num[1]);
			assertEquals(13, num[2]);
		}
		{
			final IcmChangeNumber v = IcmChangeNumber.valueOf("0.13.2");
			assertNotNull(v);
			final int[] num = v.getNumbers();
			assertNotNull(num);
			assertEquals(3, num.length);
			assertEquals(0, num[0]);
			assertEquals(13, num[1]);
			assertEquals(2, num[2]);
		}
		{
			final IcmChangeNumber v = IcmChangeNumber.valueOf(" 0.13.2 ");
			assertNotNull(v);
			final int[] num = v.getNumbers();
			assertNotNull(num);
			assertEquals(3, num.length);
			assertEquals(0, num[0]);
			assertEquals(13, num[1]);
			assertEquals(2, num[2]);
		}
		{
			final IcmChangeNumber v = IcmChangeNumber.valueOf("0.13.2.0");
			assertNotNull(v);
			final int[] num = v.getNumbers();
			assertNotNull(num);
			assertEquals(4, num.length);
			assertEquals(0, num[0]);
			assertEquals(13, num[1]);
			assertEquals(2, num[2]);
			assertEquals(0, num[3]);
		}
	}

	@Test
	public void testParseInvalid() {
		try {
			IcmChangeNumber.valueOf(null);
			fail("Expected Exception");
		} catch (NullPointerException e) {
			assertEquals("Change Number", e.getMessage());
		}
		final String[] invalidVersions = new String[] {"", ".2", "0.", "0.2.", "0..2"};
		for (String s : invalidVersions) {
			try {
				IcmChangeNumber.valueOf(s);
				fail("Expected Exception");
			} catch (IllegalArgumentException e) {
				assertEquals("Invalid change number string: " + s, e.getMessage());
			}
		}
	}

	@Test
	public void testComparator() {
		final Comparator<IcmChangeNumber> comparator = new DefaultIcmChangeNumberHandler().getComparator();
		assertEquals(0, comparator.compare(IcmChangeNumber.valueOf("0.2.2"), IcmChangeNumber.valueOf("0.2.2")));
		assertEquals(0, comparator.compare(IcmChangeNumber.valueOf("0.2.2"), IcmChangeNumber.valueOf("0.2.2.0")));
		assertEquals(0, comparator.compare(IcmChangeNumber.valueOf("0.2.2"), IcmChangeNumber.valueOf("0.2.2.0.0")));
		assertEquals(0, comparator.compare(IcmChangeNumber.valueOf("0.2.2.0"), IcmChangeNumber.valueOf("0.2.2")));
		assertEquals(1, comparator.compare(IcmChangeNumber.valueOf("0.2.3"), IcmChangeNumber.valueOf("0.2.2")));
		assertEquals(1, comparator.compare(IcmChangeNumber.valueOf("0.3.2"), IcmChangeNumber.valueOf("0.2.2")));
		assertEquals(1, comparator.compare(IcmChangeNumber.valueOf("0.4.2"), IcmChangeNumber.valueOf("0.2.2")));
		assertEquals(-1, comparator.compare(IcmChangeNumber.valueOf("0.2.2"), IcmChangeNumber.valueOf("0.2.3")));
		assertEquals(-1, comparator.compare(IcmChangeNumber.valueOf("0.2.2"), IcmChangeNumber.valueOf("0.3.2")));
		assertEquals(-1, comparator.compare(IcmChangeNumber.valueOf("0.2.2"), IcmChangeNumber.valueOf("0.4.2")));
	}
}
