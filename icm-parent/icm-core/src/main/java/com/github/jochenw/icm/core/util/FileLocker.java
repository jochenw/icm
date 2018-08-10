package com.github.jochenw.icm.core.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileLocker {
	public static interface StreamAccessor {
		InputStream getInputStream();
		OutputStream getOutputStream();
	}
	public static void runLocked(File pFile, Consumer<StreamAccessor> pConsumer) {
		final Function<StreamAccessor,Object> function = (sa) -> { pConsumer.accept(sa); return null; };
		callLocked(pFile, function);
	}
	public static <T> T callLocked(File pFile, Function<StreamAccessor,T> pConsumer) {
		try (final RandomAccessFile raf = new RandomAccessFile(pFile, "rw");
				final FileChannel channel = raf	.getChannel();
				final FileLock lock = channel.lock()) {
			final StreamAccessor sa = new StreamAccessor() {
				@Override
				public OutputStream getOutputStream() {
					try {
						channel.truncate(0);
						return Streams.uncloseable(Channels.newOutputStream(channel));
					} catch (Throwable t) {
						throw Exceptions.show(t);
					}
				}

				@Override
				public InputStream getInputStream() {
					return Streams.uncloseable(Channels.newInputStream(channel));
				}
			};
			final T t = pConsumer.apply(sa);
			return t;
		} catch (Throwable thr) {
			throw Exceptions.show(thr);
		}
	}
}
