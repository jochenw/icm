package com.github.jochenw.icm.core.api.plugins;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.apache.kahadb.util.ByteArrayInputStream;
import org.junit.Test;

import com.github.jochenw.icm.core.api.plugins.TextFileEditor;

public class TextFileEditorTest {
	@Test
	public void test() throws Exception {
		final URL centraSiteXmlUrl = getClass().getResource("centrasite.xml");
		assertNotNull(centraSiteXmlUrl);
		final URL centraSiteChangesXmlUrl = getClass().getResource("centrasite-changes.xml");
		assertNotNull(centraSiteChangesXmlUrl);
		final URL centraSiteResultXmlUrl = getClass().getResource("centrasite-result.xml");
		assertNotNull(centraSiteResultXmlUrl);
		
		final TextFileEditor tfe = new TextFileEditor();
		final Supplier<InputStream> contents = new Supplier<InputStream>() {
			@Override
			public InputStream get() {
				try {
					return centraSiteXmlUrl.openStream();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
		final Supplier<InputStream> changes = new Supplier<InputStream>() {
			@Override
			public InputStream get() {
				try {
					return centraSiteChangesXmlUrl.openStream();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		tfe.edit(contents, changes, StandardCharsets.UTF_8, null, () -> baos);

		final byte[] result = baos.toByteArray();
		try (final InputStream is1 = centraSiteResultXmlUrl.openStream();
			 final InputStream is2 = new ByteArrayInputStream(result);
			 final BufferedInputStream bis1 = new BufferedInputStream(is1);
			 final BufferedInputStream bis2 = new BufferedInputStream(is2);
		     final Reader isr1 = new InputStreamReader(bis1, StandardCharsets.UTF_8);
			 final Reader isr2 = new InputStreamReader(bis2, StandardCharsets.UTF_8);
			 final BufferedReader br1 = new BufferedReader(isr1);
			 final BufferedReader br2 = new BufferedReader(isr2)) {
			for (;;) {
				final String expect = br1.readLine();
				final String got = br2.readLine();
				if (expect == null) {
					assertNull(got);
					break;
				} else {
					assertEquals(expect, got);
				}
				
			}
		}
	}

}
