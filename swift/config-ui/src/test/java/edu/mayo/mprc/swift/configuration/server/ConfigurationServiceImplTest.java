package edu.mayo.mprc.swift.configuration.server;

import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.swift.MainFactoryContext;
import edu.mayo.mprc.swift.ResourceTable;
import edu.mayo.mprc.swift.configuration.client.model.ApplicationModel;
import edu.mayo.mprc.swift.configuration.server.session.HashStorage;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class ConfigurationServiceImplTest {
	private ConfigurationServiceImpl service;

	@BeforeMethod
	public void setup() throws Exception {
		service = new ConfigurationServiceImpl();
		service.setStorage(new HashStorage());
		service.setResourceTable((ResourceTable) MainFactoryContext.getContext().getBean("resourceTable"));
		loadDefault();
	}

	private ApplicationModel loadDefault() throws GWTServiceException {
		return service.loadFromFile(new File("this-file-does-not-exist.conf"));
	}

	@Test
	public void testLoadConfiguration() throws Exception {
		final ApplicationModel applicationModel = loadDefault();
		Assert.assertEquals(applicationModel.getDaemons().size(), 1);
		Assert.assertEquals(applicationModel.getDaemons().get(0).getChildren().size(), 4);
	}
}
