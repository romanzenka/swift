package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.swift.ui.client.rpc.ClientUser;
import edu.mayo.mprc.swift.ui.client.rpc.files.DirectoryEntry;
import edu.mayo.mprc.swift.ui.client.rpc.files.Entry;
import edu.mayo.mprc.swift.ui.client.rpc.files.ErrorEntry;
import edu.mayo.mprc.swift.ui.client.rpc.files.FileEntry;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * Test of {@link ServiceImpl}.
 */
public final class ServiceTest {
	private static final Logger LOGGER = Logger.getLogger(ServiceTest.class);

	private ServiceImpl service;

	// Folder we would try to open first when testing the service
	private static final String PREFERRED_FOLDER_NAME = "test";

	@BeforeClass
	protected void init() {
		LOGGER.debug("Starting up service");
		WebUi webUi = new WebUi();
		webUi.setBrowseRoot(new File("."));
		WebUiHolder webUiHolder = mock(WebUiHolder.class);
		stub(webUiHolder.getWebUi()).toReturn(webUi);

		service = new ServiceImpl();
		service.setWebUiHolder(webUiHolder);

		LOGGER.debug("Service is up");
	}

	@Test(groups = {"fast", "db", "integration"}, enabled = false)
	public void testListUsers() throws GWTServiceException {
		final ClientUser[] users = service.listUsers();

		Assert.assertNotNull(users, "Returned user array must not be null");
		Assert.assertTrue(users.length > 0, "There must be at least one user returned");
		LOGGER.info("Following users are present:");
		for (final ClientUser user : users) {
			LOGGER.info("\t" + user.getName() + " (" + user.getEmail() + ")");
		}
	}

	@Test(groups = {"fast", "db", "integration"}, enabled = true)
	public void testListFiles() throws GWTServiceException {
		final Entry entry = service.listFiles("", null);
		Assert.assertNotNull(entry, "There must be at least one entry returned");
		Assert.assertEquals(entry.getName(), "(root)", "The root entry has to be called (root)");
		LOGGER.info("Root folder contains these items:");
		// Remember the first dir for further tests
		DirectoryEntry firstDir = null;
		for (final Object child : entry.getChildrenList()) {
			if (child instanceof DirectoryEntry) {
				final DirectoryEntry directoryEntry = (DirectoryEntry) child;
				if (null == firstDir || directoryEntry.getName().equalsIgnoreCase(PREFERRED_FOLDER_NAME)) {
					firstDir = directoryEntry;
				}
				LOGGER.info("\t       " + directoryEntry.getName() + "/");
			} else if (child instanceof FileEntry) {
				LOGGER.info("\t       " + ((FileEntry) child).getName());
			} else if (child instanceof ErrorEntry) {
				LOGGER.info("\tERROR  " + ((ErrorEntry) child).getErrorMessage());
			} else {
				Assert.fail("Unknown entry type " + child.getClass().getName());
			}
		}
		if (firstDir == null) {
			LOGGER.warn("No folder was found, cannot test automatic folder expansion");
		} else {
			// Let us test if we can get given folder expanded (with children listed) 
			final Entry entryWithExpansion = service.listFiles("", new String[]{firstDir.getName()});
			DirectoryEntry expandedEntry = null;
			// Find our expanded folder among all the others
			for (final Object child : entryWithExpansion.getChildrenList()) {
				if (child instanceof DirectoryEntry) {
					if (((DirectoryEntry) child).getName().equals(firstDir.getName())) {
						expandedEntry = (DirectoryEntry) child;
					}
				}
			}
			Assert.assertNotNull("Could not find folder " + firstDir.getName());
			//assertFalse("There are no children in the expanded folder", expandedEntry.getChildrenList().isEmpty());
		}
	}
}
