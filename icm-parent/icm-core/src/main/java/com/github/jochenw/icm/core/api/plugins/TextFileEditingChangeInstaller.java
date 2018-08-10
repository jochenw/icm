package com.github.jochenw.icm.core.api.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.inject.Inject;

import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.prop.Interpolator;
import com.github.jochenw.icm.core.util.Objects;


@ResourceInstaller
public class TextFileEditingChangeInstaller extends AbstractChangeInstaller {
	public static final String ID = "tfe";
	public static final String PROPERTY = "tfe.file";

	@Inject private Interpolator interpolator;

	@Override
	public boolean isInstallable(IcmChangeInfo<?> pInfo) {
		return ID.equals(pInfo.getType());
	}

	@Override
	public void install(Context pContext) {
		final String property = Objects.notNull(pContext.getInfo().getAttribute("property"), PROPERTY);
		final String targetFileStr = requireProperty(property);
		final String charsetId = getProperty(property + ".charset");
		final String terminator = getProperty(property + ".lineTerminator");
		final Charset charset;
		try {
			if (charsetId == null) {
				charset = StandardCharsets.UTF_8;
			} else {
				charset = Charset.forName(charsetId);
			}
		} catch (Throwable t) {
			throw new IllegalStateException("Unable to obtain charset: " + charsetId, t);
		}
		final File targetFile = new File(targetFileStr);
		if (!targetFile.isFile()  ||  !targetFile.canRead()) {
			throw new IllegalStateException("Missing, or unreadable target file: " + targetFile.getAbsolutePath());
		}
		final Supplier<InputStream> content = new Supplier<InputStream>() {
			@Override
			public InputStream get() {
				try {
					return interpolator.filter(new FileInputStream(targetFile), charset);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
		final Supplier<InputStream> changes = new Supplier<InputStream>() {
			@Override
			public InputStream get() {
				try {
					return interpolator.filter(pContext.open(), charset);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
		final Supplier<OutputStream> target = new Supplier<OutputStream>() {
			@Override
			public OutputStream get() {
				final File backupFile = new File(targetFile.getParentFile(), targetFile.getName() + ".backup");
				if (backupFile.exists()  &&  !backupFile.delete()) {
					throw new IllegalStateException("Unable to delete backup file: " + backupFile.getAbsolutePath());
				}
				if (targetFile.exists()  &&  !targetFile.renameTo(backupFile)) {
					throw new IllegalStateException("Unable to create backup file by renaming: " + backupFile.getAbsolutePath());
				}
				try {
					return new FileOutputStream(targetFile);
				} catch (FileNotFoundException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
		final TextFileEditor tfe = getComponentFactory().requireInstance(TextFileEditor.class);
		tfe.edit(content, changes, charset, terminator, target);
	}
}
