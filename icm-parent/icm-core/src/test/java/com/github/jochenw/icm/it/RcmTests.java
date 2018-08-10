package com.github.jochenw.icm.it;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.Test;

import com.github.jochenw.icm.core.api.Icm;
import com.github.jochenw.icm.core.api.RcmBuilder;
import com.github.jochenw.icm.core.api.IcmChangeInfo;
import com.github.jochenw.icm.core.api.IcmChangeNumber;
import com.github.jochenw.icm.core.api.IcmInstallationTarget;
import com.github.jochenw.icm.core.api.cf.ComponentFactory;
import com.github.jochenw.icm.core.api.log.IcmLoggerFactory;
import com.github.jochenw.icm.core.impl.ClassPathResourceRepository;
import com.github.jochenw.icm.core.impl.XmlFileInstallationTarget;
import com.github.jochenw.icm.core.impl.cf.SimpleComponentFactory;
import com.github.jochenw.icm.core.impl.log.Log4j2LoggerFactory;
import com.github.jochenw.icm.it.test1.FooDataValidator;
import com.github.jochenw.icm.it.test2.MessageReceiver;
import com.github.jochenw.icm.it.test2.MessageSender;

public class RcmTests {
	@Test
	public void testIt0() throws Exception {
		testIt0(newRcm("test0"));
		testIt0(newRcm("test0", SimpleComponentFactory.class, null));
	}

	protected void testIt0(Icm<IcmChangeNumber> pRcm) {
		pRcm.run();
		final IcmInstallationTarget<IcmChangeNumber> target = pRcm.getInstallationTarget();
		assertEquals(0, target.getNumberOfInstalledResources(null));
	}

	@Test
	public void testIt1() throws Exception {
		testIt1(newRcm("test1"));
		testIt1(newRcm("test1", SimpleComponentFactory.class, null));
	}

	protected void testIt1(Icm<IcmChangeNumber> pRcm) {
		pRcm.run();
		final IcmInstallationTarget<IcmChangeNumber> target = pRcm.getInstallationTarget();
		assertEquals(3, target.getNumberOfInstalledResources(null));
		assertInstalled(target, "Create Table", "sql", "0.0.1");
		assertInstalled(target, "Insert Foo Data", "sql", "0.0.2");
		assertInstalled(target, "FooDataValidator", "class:" + FooDataValidator.class.getName(), "0.0.3");
	}
	
	@Test
	public void testIt2() throws Exception {
		testIt2(newRcm("test2"));
		testIt2(newRcm("test2", SimpleComponentFactory.class, null));
	}

	protected void testIt2(Icm<IcmChangeNumber> pRcm) {
		pRcm.run();
		final IcmInstallationTarget<IcmChangeNumber> target = pRcm.getInstallationTarget();
		assertEquals(3, target.getNumberOfInstalledResources(null));
		assertInstalled(target, "Create TestQueue queue", "activemq", "0.0.1");
		assertInstalled(target, "ActiveMQ Sender", "class:" + MessageSender.class.getName(), "0.0.2");
		assertInstalled(target, "ActiveMQ Receiver", "class:" + MessageReceiver.class.getName(), "0.0.3");
	}

	@Test
	public void testIt3() throws Exception {
		final Consumer<RcmBuilder<IcmChangeNumber>> buildParticipant = new Consumer<RcmBuilder<IcmChangeNumber>>() {
			@Override
			public void accept(RcmBuilder<IcmChangeNumber> pBuilder) {
				final String prefix = "com/github/jochenw/rcm/it/test1";
				final Predicate<String> filter = new Predicate<String>() {
					@Override
					public boolean test(String t) {
						return t.startsWith(prefix)  &&  t.contains("FooDataValidator");
					}
				};
				final ClassPathResourceRepository<IcmChangeNumber> repository = new ClassPathResourceRepository<IcmChangeNumber>(filter, StandardCharsets.UTF_8);
				pBuilder.resourceRepository(repository);
			}
		};
		testIt3(newRcm("test3", null, buildParticipant));
		testIt3(newRcm("test3", SimpleComponentFactory.class, buildParticipant));
	}

	protected void testIt3(Icm<IcmChangeNumber> pRcm) {
		pRcm.run();
		final IcmInstallationTarget<IcmChangeNumber> target = pRcm.getInstallationTarget();
		assertEquals(3, target.getNumberOfInstalledResources(null));
		assertInstalled(target, "Create Table", "sql", "0.0.1");
		assertInstalled(target, "Insert Foo Data", "groovy", "0.0.2");
		assertInstalled(target, "FooDataValidator", "class:" + FooDataValidator.class.getName(), "0.0.3");
	}

	@Test
	public void testIt4() throws Exception {
		testIt4(newRcm("test4"));
		testIt4(newRcm("test4", SimpleComponentFactory.class, null));
	}

	protected void testIt4(Icm<IcmChangeNumber> pRcm) {
		pRcm.run();
		final IcmInstallationTarget<IcmChangeNumber> target = pRcm.getInstallationTarget();
		assertEquals(1, target.getNumberOfInstalledResources(null));
		assertInstalled(target, "Ping Localhost", "exec", "0.0.1");
	}
	protected Icm<IcmChangeNumber> newRcm(String pTestId) {
		return newRcm(pTestId, null, null);
	}

	protected Icm<IcmChangeNumber> newRcm(String pTestId, Class<? extends ComponentFactory> pComponentFactoryClass, Consumer<RcmBuilder<IcmChangeNumber>> pBuildParticipant) {
		final RcmBuilder<IcmChangeNumber> rcmb = Icm.builder();
		if (pComponentFactoryClass != null) {
			rcmb.getComponentFactoryBuilder().componentFactoryClass(pComponentFactoryClass);
		}
		final IcmLoggerFactory rlf = new Log4j2LoggerFactory();
		rcmb.loggerFactory(rlf);
		final File resourceDir = new File("src/test/resources/com/github/jochenw/icm/it/" + pTestId);
		assertTrue(resourceDir.isDirectory());
		final String testsDir = "target/it-tests";
		final File targetDir = new File(testsDir, pTestId);
		if (!targetDir.isDirectory()  &&  !targetDir.mkdirs()) {
			throw new IllegalStateException("Unable to create target directory: " + targetDir.getAbsolutePath());
		}
		final File dbDir = new File(targetDir, "db");
		if (dbDir.isDirectory()) {
			delete(dbDir);
		}
		final File resourceListFile = new File(targetDir, "installedResources.xml");
		if (resourceListFile.isFile()  &&  !resourceListFile.delete()) {
			throw new IllegalStateException("Unable to delete resource list file: " + resourceListFile.getAbsolutePath());
		}
		final File propertyFile = new File(resourceDir, "test.properties");
		if (!propertyFile.isFile()) {
			throw new IllegalStateException("No such property file: " + propertyFile.getAbsolutePath());
		}
		final Properties props = new Properties();
		props.put("tests.dir", testsDir);
		props.put("dbDir", dbDir.getPath());
		
		rcmb
				.properties(props)
				.properties(propertyFile)
				.classPathResourceRepository("com.github.jochenw.rcm.it." + pTestId)
				.installationTarget(new XmlFileInstallationTarget<IcmChangeNumber>(resourceListFile));
		if (pBuildParticipant != null) {
			pBuildParticipant.accept(rcmb);
		}
		return rcmb.build();
	}

	public static void delete(File pDir) {
		if (pDir.isDirectory()) {
			final File[] files = pDir.listFiles();
			for (File f : files) {
				if (f.isFile()) {
					if (!f.delete()) {
						throw new IllegalStateException("Unable to delete file: " + f.getAbsolutePath());
					}
				} else if (f.isDirectory()) {
					delete(f);
				}
			}
		}
		if (!pDir.delete()) {
			throw new IllegalStateException("Unable to delete directoty: " + pDir.getAbsolutePath());
		}
	}

	public static void assertInstalled(IcmInstallationTarget<IcmChangeNumber> pTarget, String pName, String pType, String pVersion) {
		final IcmChangeNumber version = IcmChangeNumber.valueOf(pVersion);
		final IcmChangeInfo<IcmChangeNumber> info = new IcmChangeInfo<IcmChangeNumber>() {
			@Override
			public String getTitle() {
				return pName;
			}

			@Override
			public IcmChangeNumber getVersion() {
				return version;
			}

			@Override
			public String getType() {
				return pType;
			}

			@Override
			public String getAttribute(String pKey) {
				return null;
			}
		};
		assertTrue(pTarget.isInstalled(null, info));
	}
}
