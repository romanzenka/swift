package edu.mayo.mprc.swift.configuration.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.KeyExtractingWriter;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ServiceConfig;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.swift.MainFactoryContext;
import edu.mayo.mprc.swift.ResourceTable;
import edu.mayo.mprc.swift.configuration.client.model.*;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.xtandem.XTandemWorker;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public final class ConfigurationDataTest {

	private static ConfigurationData data;
	private static final String USERNAME = "username";
	private static final String TEST = "test";

	@BeforeClass
	public static void setup() throws GWTServiceException {
		data = new ConfigurationData((ResourceTable) MainFactoryContext.getContext().getBean("resourceTable"));
		data.loadDefaultConfig();
	}

	@Test
	public static void shouldProduceDefault() {
		final ApplicationModel model = data.getModel();

		Assert.assertEquals(model.getDaemons().size(), 1, "There should be one daemon");
		final DaemonModel daemon = model.getDaemons().get(0);

		Assert.assertEquals(daemon.getName(), "main", "The daemon is called 'main'");

		Assert.assertEquals(daemon.getTempFolderPath(), "var/tmp", "Default temp folder is in var/tmp");
		Assert.assertEquals(daemon.getDumpFolderPath(), "var/tmp/dump", "Default dump folder is in var/tmp/dump");
		Assert.assertEquals(daemon.isDumpErrors(), false, "By default do not dump errors");

		Assert.assertEquals(daemon.getChildren().size(), 4, "Daemon has Swift, Database, WebUI and Messenger modules by default");

		final ResourceModel swiftModule = daemon.getChildren().get(0);
		Assert.assertEquals(swiftModule.getProperty("fastaPath"), "var/fasta", "The default value has to be set");

		final List<ResourceConfig> resources = getMainDaemon().getResources();
		ResourceConfig databaseConfig = null;
		for (final ResourceConfig resourceConfig : resources) {
			if (resourceConfig instanceof Database.Config) {
				databaseConfig = resourceConfig;
				break;
			}
		}
		Assert.assertNotNull(databaseConfig, "Missing database configuration");
		final String databaseId = data.getId(databaseConfig);
		Assert.assertEquals(swiftModule.getProperty("database"), databaseId, "The database has to refer to actual database module");

		final SwiftSearcher.Config swiftSearcherConfig = (SwiftSearcher.Config) getMainDaemon().getServices().get(0).getRunner().getWorkerConfiguration();
		Assert.assertEquals(swiftSearcherConfig.getDatabase(), databaseConfig, "The database config does not match");
	}

	@Test
	public static void shouldSaveConfig() {
		final File folder = FileUtilities.createTempFolder();
		data.saveConfig(folder);
		Assert.assertTrue(new File(folder, "conf/").exists(), "Configuration folder must exist");
		Assert.assertTrue(new File(folder, "conf/swift.conf").exists(), "swift.conf config must exist");
		Assert.assertTrue(
				new File(folder, "main-run.bat").exists()
						|| new File(folder, "main-run.sh").exists(), "Main executable must exist");
		FileUtilities.quietDelete(folder);
	}

	@Test
	public static void shouldSetProperty() {
		final Database.Config dbConfig = (Database.Config) getMainDaemon().getResources().get(1);
		final UiChangesReplayer uiChangesReplayer = data.setProperty(dbConfig, USERNAME, TEST, false);
		uiChangesReplayer.replay(new UiChanges() {
			private static final long serialVersionUID = 8556656441048925831L;

			@Override
			public void setProperty(final String resourceId, final String propertyName, final String newValue) {
				Assert.assertEquals(propertyName, USERNAME);
				Assert.assertEquals(newValue, TEST);
			}

			@Override
			public void displayPropertyError(final String resourceId, final String propertyName, final String error) {
				Assert.fail("this method should not be called");
			}
		});
		final ResourceModel databaseModel = data.getModel().getDaemons().get(0).getChildren().get(1);
		Assert.assertEquals(databaseModel.getProperty(USERNAME), TEST);
	}

	@Test
	public static void shouldChangeDaemonParams() throws GWTServiceException {
		data.createChild(data.getId(getMainDaemon()), XTandemWorker.TYPE);

		// Tandem is the last service
		final List<ServiceConfig> services = getMainDaemon().getServices();
		final ResourceConfig tandem = services.get(services.size() - 1).getRunner().getWorkerConfiguration();

		// Switch the daemon to Linux
		data.setProperty(getMainDaemon(), DaemonConfig.OS_NAME, "Linux", false);
		final UiChangesReplayer changes = data.setProperty(getMainDaemon(), DaemonConfig.OS_ARCH, "x86", false);

		// Config has to change
		checkTandemExecutable(tandem, "bin/tandem/linux_redhat_tandem/tandem.exe");

		// The change has to be reflected to the UI
		changes.replay(new UiChanges() {
			private static final long serialVersionUID = -2006104357034795782L;

			@Override
			public void setProperty(final String resourceId, final String propertyName, final String newValue) {
				Assert.assertEquals(propertyName, XTandemWorker.TANDEM_EXECUTABLE);
				Assert.assertEquals(newValue, "bin/tandem/linux_redhat_tandem/tandem.exe");
			}

			@Override
			public void displayPropertyError(final String resourceId, final String propertyName, final String error) {
				Assert.fail();
			}
		});

		// Change to Windows
		data.setProperty(getMainDaemon(), DaemonConfig.OS_NAME, "Windows", false);
		data.setProperty(getMainDaemon(), DaemonConfig.OS_ARCH, "32-bit", false);

		// Config has to change
		checkTandemExecutable(tandem, "bin/tandem/win32_tandem/tandem.exe");
	}

	@Test
	public static void shouldAddChild() throws GWTServiceException {
		final int children1 = getMainDaemonChildrenCount();
		final ResourceModel model1 = data.createChild(data.getId(getMainDaemon()), XTandemWorker.TYPE);
		final int children2 = getMainDaemonChildrenCount();
		final ResourceModel model2 = data.createChild(data.getId(getMainDaemon()), XTandemWorker.TYPE);
		final int children3 = getMainDaemonChildrenCount();
		Assert.assertTrue(model1.getId() != model2.getId(), "Different models must have different names");
		final String service1 = getServiceForModel(model1).getName();
		final String service2 = getServiceForModel(model2).getName();
		Assert.assertFalse(service1.equals(service2), "Different services need different names");
		Assert.assertEquals(children1 + 1, children2);
		Assert.assertEquals(children2 + 1, children3);
	}

	private static ServiceConfig getServiceForModel(final ResourceModel model1) {
		final ResourceConfig resourceConfig = data.getResourceConfig(model1.getId());
		for (final DaemonConfig daemonConfig : data.getConfig().getDaemons()) {
			for (final ServiceConfig serviceConfig : daemonConfig.getServices()) {
				if (serviceConfig.getRunner().getWorkerConfiguration() == resourceConfig) {
					return serviceConfig;
				}
			}
		}
		throw new MprcException("Resource config not found in this application");
	}

	private static int getMainDaemonChildrenCount() {
		return getMainDaemon().getServices().size() + getMainDaemon().getResources().size();
	}

	@Test
	public static void shouldRemoveChild() throws GWTServiceException {
		final int children1 = getMainDaemonChildrenCount();
		final ResourceModel model1 = data.createChild(data.getId(getMainDaemon()), XTandemWorker.TYPE);
		final int children2 = getMainDaemonChildrenCount();
		data.removeChild(model1.getId());
		final int children3 = getMainDaemonChildrenCount();
		Assert.assertEquals(children1 + 1, children2);
		Assert.assertEquals(children2 - 1, children3);
	}

	private static void checkTandemExecutable(final ResourceConfig tandem, final String expected) {
		Assert.assertEquals(KeyExtractingWriter.get(tandem, XTandemWorker.TANDEM_EXECUTABLE), expected);
	}

	private static DaemonConfig getMainDaemon() {
		return data.getConfig().getDaemons().get(0);
	}
}
