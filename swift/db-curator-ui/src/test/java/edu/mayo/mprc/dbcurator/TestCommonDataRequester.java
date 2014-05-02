package edu.mayo.mprc.dbcurator;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.client.steppanels.CurationStepStub;
import edu.mayo.mprc.dbcurator.client.steppanels.CurationStub;
import edu.mayo.mprc.dbcurator.client.steppanels.ManualInclusionStepStub;
import edu.mayo.mprc.dbcurator.client.steppanels.SequenceManipulationStepStub;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationContext;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.dbcurator.server.AttributeStore;
import edu.mayo.mprc.dbcurator.server.CommonDataRequesterLogic;
import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Roman Zenka
 */
public final class TestCommonDataRequester {

	private CommonDataRequesterLogic requester;
	private CurationContext context;
	private File tempFolder;
	private CurationDao curationDao;

	@BeforeTest
	public void setup() {
		requester = new CommonDataRequesterLogic();
		requester.setAttributeStore(new AttributeStore() {
			private final Map<String, Object> session = new HashMap<String, Object>(10);

			@Override
			public Object getAttribute(final String name) {
				return session.get(name);
			}

			@Override
			public void setAttribute(final String name, final Object value) {
				session.put(name, value);
			}
		});

		// Let's make a curation dao that will refuse to save curation with notes too long
		curationDao = mock(CurationDao.class);
		doThrow(new MprcException("Notes too long")).when(curationDao).addCuration(any(Curation.class));

		requester.setCurationDao(curationDao);

		tempFolder = FileUtilities.createTempFolder();

		context = new CurationContext();

		final File fastaFolder = new File(tempFolder, "fasta");
		final File fastaUploadFolder = new File(tempFolder, "upload");
		final File fastaArchiveFolder = new File(tempFolder, "archive");
		final File localTempFolder = new File(tempFolder, "temp");
		FileUtilities.ensureFolderExists(fastaFolder);
		FileUtilities.ensureFolderExists(fastaUploadFolder);
		FileUtilities.ensureFolderExists(fastaArchiveFolder);
		FileUtilities.ensureFolderExists(localTempFolder);

		context.initialize(fastaFolder, fastaUploadFolder, fastaArchiveFolder, localTempFolder);

		requester.setCurationContext(context);
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(tempFolder);
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldReportDatabaseNotesTooLong() {
		final CurationStub curationStub = new CurationStub();
		curationStub.setId(1);
		curationStub.setDecoyRegex("Rev_");
		curationStub.setNotes("hello");
		curationStub.setOwnerEmail("test@testing.com");
		curationStub.setPathToResult(new File(context.getLocalTempFolder(), "result.fasta").getAbsolutePath());
		curationStub.setShortName("testfasta");

		final ManualInclusionStepStub includeSequence = new ManualInclusionStepStub();
		includeSequence.header = "hello_protein";
		includeSequence.sequence = "GGGGGAAAAAAKKKKKK";

		final SequenceManipulationStepStub reverse = new SequenceManipulationStepStub();
		reverse.manipulationType = SequenceManipulationStepStub.REVERSAL;
		reverse.overwrite = false;

		final List<CurationStepStub> stubs = new ArrayList<CurationStepStub>(2);
		stubs.add(includeSequence);
		stubs.add(reverse);
		curationStub.setSteps(stubs);

		final CurationStub result = requester.runCuration(curationStub);
	}

}
