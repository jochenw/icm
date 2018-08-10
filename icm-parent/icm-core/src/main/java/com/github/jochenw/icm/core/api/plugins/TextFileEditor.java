package com.github.jochenw.icm.core.api.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.xml.sax.InputSource;

import com.github.jochenw.icm.core.util.Exceptions;
import com.github.jochenw.icm.core.util.JaxbUtils;
import com.github.jochenw.icm.core.util.Objects;
import com.github.jochenw.icm.core.util.Strings;
import com.github.namespaces.jochenw.rcm.core.schema.tfe._1_0.Changes;
import com.github.namespaces.jochenw.rcm.core.schema.tfe._1_0.IndexableElement;
import com.github.namespaces.jochenw.rcm.core.schema.tfe._1_0.Insertion;
import com.github.namespaces.jochenw.rcm.core.schema.tfe._1_0.Replacement;

public class TextFileEditor {
	protected static class Data {
		private final Supplier<InputStream> contents;
		private final Supplier<InputStream> changes;
		private final Charset charset;
		private final String lineTerminator;
		private final Supplier<OutputStream> target;
		private final List<String> lines = new ArrayList<>();
		public Data(Supplier<InputStream> contents, Supplier<InputStream> changes, Charset charset,
				String lineTerminator, Supplier<OutputStream> target) {
			this.contents = contents;
			this.changes = changes;
			this.charset = charset;
			this.lineTerminator = lineTerminator;
			this.target = target;
		}

		public Supplier<InputStream> getContents() {
			return contents;
		}
		public Supplier<InputStream> getChanges() {
			return changes;
		}
		public Charset getCharset() {
			return charset;
		}
		public String getLineTerminator() {
			return lineTerminator;
		}
		public Supplier<OutputStream> getTarget() {
			return target;
		}
		public List<String> getLines() {
			return lines;
		}
	}
	public void edit(Supplier<InputStream> pContents, Supplier<InputStream> pChanges, Charset pCharset, String pLineTerminator, Supplier<OutputStream> pTarget) {
		Objects.requireNonNull(pContents, "Contents");
		Objects.requireNonNull(pChanges, "Changes");
		final Charset charset = Objects.notNull(pCharset, StandardCharsets.UTF_8);
		Objects.requireNonNull(pTarget, "Target");
		final Data data = new Data(pContents, pChanges, charset, pLineTerminator, pTarget);
		edit(data);
	}

	public void edit(Data pData) {
		try {
			read(pData);
			apply(pData);
			write(pData);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
	
	public void read(Data pData) throws IOException {
		try (final InputStream in = pData.contents.get();
			 final BufferedInputStream bis = new BufferedInputStream(in);
			 final InputStreamReader isr = new InputStreamReader(bis, pData.getCharset())) {
			toStringList(isr, pData.getLineTerminator(), (s) -> pData.getLines().add(s));
		}
			 
	}

	private void toStringList(Reader pReader, String pLineTerminator, Consumer<String> pConsumer) throws IOException {
		try (BufferedReader br = new BufferedReader(pReader)) {
			for (;;) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				if (pLineTerminator != null  &&  line.endsWith(pLineTerminator)) {
					line = line.substring(0, line.length()-pLineTerminator.length());
				}
				pConsumer.accept(line);
			}
		}
	}
	
	public void write(Data pData) throws IOException {
		final String lineTerminator = pData.getLineTerminator();
		try (OutputStream os = pData.getTarget().get();
			 BufferedOutputStream bos = new BufferedOutputStream(os);
			 OutputStreamWriter osw = new OutputStreamWriter(bos, pData.getCharset());
			 BufferedWriter bw = new BufferedWriter(osw)) {
			for (String s : pData.getLines()) {
				bw.write(s);
				if (lineTerminator == null) {
					bw.newLine();
				} else {
					bw.write(lineTerminator);
				}
			}
		}
	}
	
	protected void apply(Data pData) {
		try (InputStream is = pData.getChanges().get()) {
			final InputSource isource = new InputSource(is);
			final Changes changes = (Changes) JaxbUtils.parse(isource, Changes.class);
			for (IndexableElement change : changes.getReplaceOrInsert()) {
				apply(pData, change);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void apply(Data pData, IndexableElement pElement) {
		final List<String> lines = pData.getLines();
		if (pElement instanceof Replacement) {
			final Replacement replacement = (Replacement) pElement;
			final String from = replacement.getFrom();
			if (from == null  ||  from.length() == 0) {
				throw new IllegalStateException("Missing, or empty from attribute for replacement");
			}
			final int index = find(lines, from, pElement.getIndex());
			final String value = Strings.replace(lines.get(index), from, replacement.getTo());
			lines.set(index, value);
		} else if (pElement instanceof Insertion) {
			final Insertion insertion = (Insertion) pElement;
			final String find = insertion.getFind();
			if (find == null  ||  find.length() == 0) {
				throw new IllegalStateException("Missing, or empty attribute for insertion: @find");
			}
			final int index = find(lines, find, pElement.getIndex());
			final String content = insertion.getContent().getValue();
			if (content == null  ||  content.length() == 0) {
				throw new IllegalStateException("Missing, or empty content element for insertion");
			}
			final List<String> contentList = new ArrayList<>();
			try {
				toStringList(new StringReader(content), pData.getLineTerminator(), (s) -> contentList.add(s));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			switch (insertion.getLocation()) {
			  case "after":
				lines.addAll(index+1, contentList);
				break;
			  case "before":
				  lines.addAll(index, contentList);
				break;
			  default:
				throw new IllegalStateException("Invalid location value (Expected before|after): " + insertion.getLocation());
			}
		}
	}


	protected int find(List<String> pContent, String pValue, int pIndex) {
		int numberOfMatches = -1;
		int matchIndex = -1;
		for (int i = 0;  i < pContent.size();  i++) {
			if (pContent.get(i).contains(pValue)) {
				++numberOfMatches;
				if (pIndex == -1  ||  numberOfMatches == pIndex) {
					if (matchIndex == -1) {
						matchIndex = i;
					}
				}
			}
		}
		if (numberOfMatches == -1) {
			throw new IllegalStateException("Change location indicator not found: " + pValue);
		}
		if (pIndex == -1) {
			return matchIndex;
		}
		if (numberOfMatches < pIndex) {
			throw new IllegalStateException("Change location indicator was found " + numberOfMatches + " times, expected " + pIndex +": " + pValue);
		}
		return matchIndex;
	}
}
