package edu.mayo.mprc.sequest.core;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.tar.TarReader;
import edu.mayo.mprc.tar.TarWriter;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * used to test sequest submit functionality
 */
public final class TestSequestSubmission {
	private static final Logger LOGGER = Logger.getLogger(TestSequestSubmission.class);
	private static final String SEQUEST_HUNG_WINDOWS = "sequest27_master_hung.bat";
	private static final String SEQUEST_HUNG_LINUX = "sequest27_master_hung.sh";
	//String allgroups = {"linux", "windows"}
	//String linuxgroups = {"linux"}
	//String currgroups = allgroups;
	private String tenchar = "0123456789";
	private String ninechar = "01234.dta";
	private String nineoutfile = "01234.out";

	private static File getHostsFile() {
		return new File("/etc/pvmhosts");
	}

	/**
	 * case where less than a full bucket so no files should be submitted;
	 */
	@Test(enabled = true, groups = {"linux", "sequest"})
	public void testSequestSubmitUnder() {
		LOGGER.debug("in testSequestSubmitUnder");
		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();
		try {

			LOGGER.debug("in submit under");
			final SequestSubmit s = new SequestSubmit(120, new File("myparams"), folder, new File(folder, "mytar.tar"), getHostsFile());


			LOGGER.debug("maxlinelength=" + s.getMaxLineLength());

			final char c = 'a';
			final List<String> dtafilenames = new ArrayList<String>();
			for (int i = 0; i < 9; i++) {
				final String dtafilename = createDtaAndOutFile(folder);
				LOGGER.debug("dtafilename=" + dtafilename);
				dtafilenames.add(dtafilename);
			}
			// need to find the under cutoff;
			int totallength = 0;
			for (final String dtafilename : dtafilenames) {
				totallength += dtafilename.length() + 1;
			}

			s.setMaxLineLength(totallength + 1);
			int n = 1;
			LOGGER.debug("max  line length is {s.getMaxLineLength()}");
			for (final String dtafilename : dtafilenames) {
				s.addDtaFile(new File(dtafilename), false);
				LOGGER.debug("wrote " + n + ", accumulatedlength=" + s.getAccumulatedLength());
				n++;
			}
			LOGGER.debug("wrote {n-1}, accumulatedlength=" + s.getAccumulatedLength());
			// should be 9 of them in there;
			final int n1 = s.getHowManyFiles();
			LOGGER.debug("found " + n1);
			Assert.assertEquals(n1, 9);
			LOGGER.debug("testSequestSubmitUnder succeeded");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	private String createDtaAndOutFile(final File folder) {
		char c = 'a';
		try {
			final String prefix = folder.getAbsolutePath() + File.separator + c++;
			final String dtafilename = prefix + ninechar;
			final File f = new File(dtafilename);
			f.createNewFile();
			final String outfile = prefix + nineoutfile;
			final File fout = new File(outfile);
			fout.createNewFile();
			return dtafilename;
		} catch (Exception t) {
			throw new MprcException(t);
		}
	}

	/**
	 * case where have a full bucket so should be submitte and bucket emptied;
	 */
	@Test(enabled = true, groups = {"linux", "sequest"})
	public void testSequestSubmitOver() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("in testSequestSubmitOver");

		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();
		try {


			final SequestSubmit s = new SequestSubmit(100, new File("myparams"), folder, new File(folder, "mytar.tar"), getHostsFile());

			final SequestRunnerStub scs = new SequestRunnerStub(folder, null, new ArrayList<File>(), getHostsFile());
			scs.setStartTimeOut(10 * 1000);
			scs.setWatchDogTimeOut(10 * 1000);
			s.setSequestCaller(scs);

			s.setMaxLineLength(100);


			final List<String> dtafilenames = new ArrayList<String>();
			for (int i = 0; i < 10; i++) {
				final String dtafilename = createDtaAndOutFile(folder);
				LOGGER.debug("dtafilename=" + dtafilename);
				dtafilenames.add(dtafilename);
			}
			// need to find the under cutoff;
			int totallength = 0;
			for (final String dtafilename : dtafilenames) {
				totallength += (new File(dtafilename).getName()).length() + 1;
			}
			//totallength += s.getSequestcaller().getCommand().length();
			final long fullLength = totallength - new File((dtafilenames.get(dtafilenames.size() - 1))).length() - 1;
			s.setMaxLineLength((int) fullLength);
			for (final String dtafilename : dtafilenames) {
				s.addDtaFile(new File(dtafilename), false);
			}
			LOGGER.debug("wrote 10");
			// should be 1 of them in there;
			final int n = s.getHowManyFiles();
			Assert.assertEquals(n, 1);
			LOGGER.debug("testSequestSubmitOver succeeded");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	/**
	 * case where have a full bucket so should be submitte and bucket emptied;
	 */
	@Test(enabled = false, groups = {"windows", "sequest"})
	public void testSequestSubmitOverWindows() {
		LOGGER.debug("in testSequestSubmitOverWindows");

		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();
		try {

			final SequestSubmit s = new SequestSubmit(100, new File("myparams"), folder, new File(folder, "mytar.tar"), getHostsFile());

			final SequestRunnerStub sc = new SequestRunnerStub(folder, null, new ArrayList<File>(), getHostsFile());
			sc.setWatchDogTimeOut(10 * 1000);
			sc.setStartTimeOut(10 * 1000);

			//sc.setCall(getWindowsCall());
			// "cmd /c echo hi"
			sc.setCommand("cmd");
			final List<String> args = new ArrayList<String>();
			args.add("/c");
			args.add("echo");
			args.add("hi");
			sc.setArgs(args);

			s.setSequestCaller(sc);


			final char c = 'a';


			s.setMaxLineLength(100);

			for (int i = 0; i < 10; i++) {

				final String dtafilename = createDtaAndOutFile(folder);
				s.addDtaFile(new File(dtafilename), false);
			}
			LOGGER.debug("wrote 10");
			// should be 0 of them in there;
			final int n = s.getHowManyFiles();
			Assert.assertEquals(n, 1);
			LOGGER.debug("testSequestSubmitOver succeeded");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	/**
	 * case where force it to submit to sequest;
	 */
	@Test(enabled = false, groups = {"windows", "sequest"})
	public void testSequestSubmitOneForcedWindows() {
		LOGGER.debug("in testSequestSubmitOneForcedWindows");

		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();

		try {

			final SequestSubmit s = getSequestSubmit(folder);
			s.setMaxLineLength(100);
			final SequestRunnerStub sc = getSequestRunnerStub(folder);
			sc.setStartTimeOut(10 * 1000);
			sc.setWatchDogTimeOut(10 * 1000);

			sc.setCommand("cmd");
			final List<String> args = new ArrayList<String>();
			args.add("/c");
			args.add("echo");
			args.add("hi");
			sc.setArgs(args);
			s.setSequestCaller(sc);


			char c = 'a';

			final File f = new File(folder.getAbsolutePath() + File.separator + c++ + ninechar);
			f.createNewFile();


			final String dtafilename = createDtaAndOutFile(folder);
			s.addDtaFile(new File(dtafilename), true);

			LOGGER.debug("wrote 1");
			// should be 0 of them in there;
			final int n = s.getHowManyFiles();
			Assert.assertEquals(n, 0);
			LOGGER.debug("testSequestSubmitOneForced succeeded");

		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	private SequestSubmit getSequestSubmit(final File folder) {
		return new SequestSubmit(100, new File("myparams"), folder, new File(folder, "mytar.tar"), getHostsFile());
	}

	/**
	 * case where force it to submit to sequest;
	 */
	@Test(enabled = true, groups = {"linux", "sequest"})
	public void testSequestSubmitOneForced() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("in testSequestSubmitOneForced");


		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();
		try {

			final SequestSubmit s = getSequestSubmit(folder);
			s.setMaxLineLength(100);
			final SequestRunnerStub scs = getSequestRunnerStub(folder);
			scs.setStartTimeOut(10 * 1000);
			scs.setWatchDogTimeOut(10 * 1000);
			s.setSequestCaller(scs);


			char c = 'a';

			final File f = new File(folder.getAbsolutePath() + File.separator + c++ + ninechar);
			f.createNewFile();


			final String dtafilename = createDtaAndOutFile(folder);
			s.addDtaFile(new File(dtafilename), true);

			LOGGER.debug("wrote 1");
			// should be 0 of them in there;
			final int n = s.getHowManyFiles();
			Assert.assertEquals(n, 0);
			LOGGER.debug("testSequestSubmitOneForced succeeded");

		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	public String getWindowsCall() {
		return "cmd /c echo hi";
	}

	public String getCall() {
		return "cd .";
	}


	/**
	 * case where force it to submit to sequest;
	 */
	@Test(enabled = true, groups = {"linux", "sequest"})
	public void testionsSectionProcessLine() {
		LOGGER.debug("in testionsSectionProcessLine");

		final File folder = FileUtilities.createTempFolder();
		final SequestSubmitterInterface si = new SequestSubmitStub(1, 1, 1, "myparams", folder.getAbsolutePath(), folder.getAbsolutePath() + File.separator + "mytar.tar");

		processLineCode(si, folder);

	}


	/**
	 * case where force it to submit to sequest;
	 */
	@Test(enabled = false, groups = {"windows", "sequest"})
	public void testionsSectionProcessLineWindows() {
		LOGGER.debug("in testionsSectionProcessLineWindows");


		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();

		try {
			final SequestSubmitStub si = new SequestSubmitStub(1, 1, 1, "myparams", folder.getAbsolutePath(), folder.getAbsolutePath() + File.separator + "mytar.tar");


			final SequestRunnerStub sc = getSequestRunnerStub(folder);
			sc.setStartTimeOut(10 * 1000);
			sc.setWatchDogTimeOut(10 * 1000);

			sc.setCommand("cmd");
			final List<String> args = new ArrayList<String>();
			args.add("/c");
			args.add("echo");
			args.add("hi");
			sc.setArgs(args);
			si.setSequestCaller(sc);

			processLineCode(si, folder);

		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}


	private void processLineCode(final SequestSubmitterInterface si, final File folder) {
		final MgfIonsModeller i = new MgfIonsModeller();

		i.setSequestSubmitter(si);

		final String folderName = folder.getAbsolutePath();
		i.setWorkingDir(folderName);
		i.setMgfFileName("mymgf.mgf");

		i.processLine("PEPMASS=1.1");

		String result = i.getMz();

		LOGGER.debug("value is " + result);

		Assert.assertNotNull(result, "mz is null");

		i.processLine("CHARGE=2");

		LOGGER.debug("charge=" + i.getCharge());

		Assert.assertNotNull(i.getCharge(), "charge is null");
		Assert.assertEquals(i.getCharge(), "2", "expected 2 but got " + i.getCharge());

		i.processLine("TITLE=test_1199-1302_46GB0402spot46 scan 10 10 (test_1199-1302_46GB0402spot46.10.10.3.dta)");

		i.processLine("END IONS");

		Assert.assertEquals(i.done, true, "END IONS should set done=true");

		i.processLine("BEGIN IONS");

		result = i.getMz();

		Assert.assertNull(i.getMz(), "BEGIN IONS should null the mz");
	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 * case where test processing the title line;
	 */
	public void testTitle() {
		LOGGER.debug("running testTitle");

		final MgfIonsModeller i = new MgfIonsModeller();

		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();

		i.setWorkingDir(folderName);

		i.setMgfFileName("mymgf.mgf");

		i.processLine("TITLE=test_1199-1302_46GB0402spot46 scan 10 10 (test_1199-1302_46GB0402spot46.10.10.3.dta)");
	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 * test dealing with one ions section;
	 */
	public void testMgffileParserOneIonsSection() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("running testMgffileParserOneIonsSection");
		final String[] lines = {
				"BEGIN IONS",
				"TITLE=test_1199-1302_46GB0402spot46 scan 10 10 (test_1199-1302_46GB0402spot46.10.10.3.dta)",
				"CHARGE=3+",
				"PEPMASS=771.99743764",
				"222.99 25.1",
				"224.37 6.9",
				"226.08 25.9",
				"227.07 162.2",
				"255.09 10.4",
				"255.70 5.5",
				"257.09 1.5",
				"259.14 665.8",
				"END IONS",
		};


		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();

		try {

			// create a .out file with test_1199-1302_46GB0402spot46.10.10.3.out;
			final File fout = new File(folderName + File.separator + "test_1199-1302_46GB0402spot46.10.10.3.out");
			fout.createNewFile();


			final SequestSubmitterInterface s = getSequestSubmit(folder);

			final SequestRunnerStub scs = getSequestRunnerStub(folder);
			scs.setStartTimeOut(10 * 1000);
			scs.setWatchDogTimeOut(10 * 1000);
			s.setSequestCaller(scs);

			final IonsModellerInterface i = new MgfIonsModeller();


			final MgfToDtaFileParser parser = new MgfToDtaFileParser(s, i, folderName);

			parser.setMgfFileName("mymgf.mgf");

			String all = "";
			for (final String line : lines) {
				all += line + "\n";
			}
			final StringReader r = new StringReader(all);
			final BufferedReader br = new BufferedReader(r);
			try {
				parser.getDTAsFromFile(br);
			} catch (Exception t) {
				Assert.fail("parser failed", t);
			}


		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 * test dealing with 2 ions sections;
	 */
	public void testMgffileParserTwoIonsSections() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("running testMgffileParserTwoIonsSections");
		final String[] lines = {
				"BEGIN IONS",
				"TITLE=test_1199-1302_46GB0402spot46 scan 10 10 (test_1199-1302_46GB0402spot46.10.10.3.dta)",
				"CHARGE=3+",
				"PEPMASS=771.99743764",
				"222.99 25.1",
				"224.37 6.9",
				"226.08 25.9",
				"227.07 162.2",
				"255.09 10.4",
				"255.70 5.5",
				"257.09 1.5",
				"259.14 665.8",
				"END IONS",
				"BEGIN IONS",
				"TITLE=test_1199-1303_46GB0402spot46 scan 11 11 (test_1199-1302_46GB0402spot46.11.11.3.dta)",
				"CHARGE=3+",
				"PEPMASS=771.99743764",
				"222.99 25.1",
				"224.37 6.9",
				"226.08 25.9",
				"227.07 162.2",
				"255.09 10.4",
				"255.70 5.5",
				"257.09 1.5",
				"259.14 665.8",
				"END IONS",
		};

		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();
		try {

			// create a .out file with test_1199-1302_46GB0402spot46.10.10.3.out;
			final File fout = new File(folderName + File.separator + "test_1199-1302_46GB0402spot46.10.10.3.out");
			fout.createNewFile();

			// create a .out file with test_1199-1302_46GB0402spot46.11.11.3.out;
			final File fout1 = new File(folderName + File.separator + "test_1199-1302_46GB0402spot46.11.11.3.out");
			fout1.createNewFile();


			final SequestSubmitterInterface s = getSequestSubmit(folder);

			final SequestRunnerStub scs = getSequestRunnerStub(folder);
			scs.setStartTimeOut(10 * 1000);
			scs.setWatchDogTimeOut(10 * 1000);
			s.setSequestCaller(scs);


			final IonsModellerInterface i = new MgfIonsModeller();


			final MgfToDtaFileParser parser = new MgfToDtaFileParser(s, i, folderName);

			parser.setMgfFileName("mymgf.mgf");

			String all = "";
			for (final String line : lines) {
				all = all + line + "\n";
			}
			final StringReader r = new StringReader(all);
			final BufferedReader br = new BufferedReader(r);
			try {
				parser.getDTAsFromFile(br);
			} catch (Exception t) {
				Assert.fail("parser failed", t);
			}

		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	private SequestRunnerStub getSequestRunnerStub(final File folder) {
		return new SequestRunnerStub(folder, null, new ArrayList<File>(0), getHostsFile());
	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 * test dealing with 2 dta file names;
	 */
	public void testSequestRunnerSetuptwofiles() {
		LOGGER.debug("running testSequestRunnerSetuptwofiles");

		final File folder = FileUtilities.createTempFolder();
		final String folderName = folder.getAbsolutePath();

		try {

			final List<File> dtafiles;

			dtafiles = new ArrayList<File>();
			dtafiles.add(new File("mydta1.dta"));
			dtafiles.add(new File("mydta2.dta"));

			try {

				final SequestRunner c = getSequestRunner(folder, dtafiles);

			} catch (MprcException m) {
				Assert.fail("testSequestRunnerSetuptwofiles failed", m);
			}


		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}


	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 * test dealing with one dta file name;
	 */
	public void testSequestRunnerSetuponefile() {
		LOGGER.debug("running testSequestRunnerSetuponefile");

		final File folder = FileUtilities.createTempFolder();

		final List<File> dtafiles;

		dtafiles = new ArrayList<File>();
		dtafiles.add(new File("mydta1.dta"));

		try {

			final SequestRunner c = getSequestRunner(folder, dtafiles);

		} catch (MprcException m) {
			Assert.fail("testSequestRunnerSetuponefile failed", m);
		}
	}

	private SequestRunner getSequestRunner(final File temp, final List<File> dtafiles) {
		return new SequestRunner(temp, new File("myparams"), dtafiles, getHostsFile());
	}


	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 *  test case where no dta file names provided;
	 **/
	public void testSequestRunnernofiles() {
		LOGGER.debug("running testSequestRunnernofiles");

		final File folder = FileUtilities.createTempFolder();

		final SequestRunner c;


		try {
			c = getSequestRunner(folder, new ArrayList<File>());
			c.getCall();
		} catch (MprcException m) {
			if (!m.getMessage().contains(SequestRunner.NO_DTA_FILES_PASSED)) {
				Assert.fail("testSequestRunnernofiles failed", m);
				return;
			}
			return;
		}
		Assert.fail("Exception should have been generated with content " + SequestRunner.NO_DTA_FILES_PASSED);
	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	public void testSequestRunneronedtanoparams() {
		LOGGER.debug("running testSequestRunneronedtanoparams");

		final File folder = FileUtilities.createTempFolder();

		final List<File> dtafiles;

		dtafiles = new ArrayList<File>();
		dtafiles.add(new File("mydta.dta"));

		final SequestRunner c;


		try {

			c = new SequestRunner(folder, null, dtafiles, getHostsFile());
			c.getCall();

		} catch (MprcException m) {
			if (!m.getMessage().contains(SequestRunner.NO_PARAMS_FILE_PASSED)) {
				Assert.fail("testSequestRunneronedtanoparams failed", m);
			}
			return;
		}
		Assert.fail("Exception should have been generated with content " + SequestRunner.NO_PARAMS_FILE_PASSED);
	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	/**
	 * test caller the caller with a real call (on linux or windows);
	 */
	public void testSequestRunnerWithExistingCall() {
		LOGGER.debug("running testSequestRunnerWithExistingCall");

		final File folder = FileUtilities.createTempFolder();

		final List<File> dtafiles;

		dtafiles = new ArrayList<File>();
		dtafiles.add(new File("mydta.dta"));

		File params = null;
		try {
			params = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);
			final SequestRunner sequestRunner = new SequestRunnerStub(folder, params, dtafiles, getHostsFile());

			sequestRunner.setStartTimeOut(10 * 1000);
			sequestRunner.setWatchDogTimeOut(10 * 1000);
			sequestRunner.setCommand("echo");
			final List<String> args = new ArrayList<String>();
			args.add("hi");
			final String call = sequestRunner.getCall();
			Assert.assertEquals(call.trim(), "echo hi", "getCall not set properly");
			final Thread t = new Thread(sequestRunner);
			t.setUncaughtExceptionHandler(new ExceptionHandler());
			t.start();
			t.join(10000);
		} catch (Exception e) {
			Assert.fail("testSequestRunnerWithExistingCall failed", e);
		} finally {
			FileUtilities.cleanupTempFile(params);
		}
	}

	class ExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(final Thread thread, final Throwable throwable) {
			Assert.fail("thread failed", throwable);
		}
	}

	@Test(enabled = true, groups = {"linux", "sequest"})
	public void testMGF2SequestScriptStubbed() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("running testMGF2SequestScriptStubbed");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();

		try {
			// set up the environment so can grab these;
			// actually is;
			final File mgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final File params = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);

			final Mgf2SequestCallerStubbed m = new Mgf2SequestCallerStubbed();

			m.setHostsFile(getHostsFile());

			m.callSequest(new File(outputDir, "tarfile.tar"), params, mgf, 10 * 1000, 10 * 1000, hdrFile);


		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	@Test(enabled = false, groups = {"sequest"})
	public void testMGF2SequestScriptSequest27() {
		LOGGER.debug("running testMGF2SequestScriptSequest27");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {
			// set up the environment so can grab these;
			// actually is;
			final File mgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File params = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final Mgf2SequestCaller m = new Mgf2SequestCaller();
			m.setSequestExe("c:sequestsequest27.exe");
			m.setHostsFile(getHostsFile());

			m.callSequest(new File(outputDir, "tarfile.tar"), params, mgf, 120 * 1000, 120 * 1000, hdrFile);

		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	/**
	 * in this case the the mgf file has two peaklists;
	 */
	@Test(enabled = true, groups = {"sequest"})
	public void testMGF2SequestScriptSequest() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("running testMGF2SequestScriptSequest");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {
			// set up the environment so can grab these;
			// actually is;
			final File fmgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final File fparams = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);

			final Mgf2SequestCaller m = new Mgf2SequestCaller();
			m.setSequestExe(getSequestExe());
			m.setHostsFile(getHostsFile());

			m.callSequest(new File(outputDir, "tarfile.tar"), fparams, fmgf, 120 * 1000, 120 * 1000, hdrFile);
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	private static boolean skipSequestTest() {
		return FileUtilities.isWindowsPlatform() || FileUtilities.isMacPlatform();
	}

	/**
	 * in this case the the mgf file has two peaklists;
	 */
	@Test(enabled = true, groups = {"sequest", "overnight"})
	public void testMGF2SequestScriptSequestLargeMgfFile() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("running testMGF2SequestScriptSequestLargeMgfFile");

		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {
			LOGGER.debug("outputDir=" + outputDir.getAbsolutePath());
			if (!outputDir.exists()) {
				Assert.fail(outputDir.getAbsolutePath() + " does not exist");
			}

			// TODO: Generate the file instead of depending on a huge file somewhere on the disk
			final File fmgf = new File("/mnt/raid1/test/ImmunostainsSPA706308.mgf");
			if (!fmgf.exists()) {
				Assert.fail(fmgf.getAbsolutePath() + " does not exist");
			}

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final File fparams = TestingUtilities.getTempFileFromResource("/sequest_LTQ.params", true, /*useSystemTempFolder*/ null);

			final Mgf2SequestCaller m = new Mgf2SequestCaller();
			m.setSequestExe(getSequestExe());
			m.setHostsFile(getHostsFile());

			m.callSequest(new File(outputDir, "tarfile.tar"), fparams, fmgf, 10 * 1000, 10 * 1000, hdrFile);
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	private String getSequestExe() {
		return null;//TestApplicationContext.getSequestCommand().getAbsolutePath();
	}


	/**
	 * in this case the the mgf file has two peaklists;
	 * the params file does not have a real database so a failure should occur;
	 */
	@Test(enabled = true, groups = {"sequest"})
	public void testMGF2SequestScriptSequestNoDatabase() {
		if (skipSequestTest()) {
			return;
		}

		LOGGER.debug("running testMGF2SequestScriptSequestNoDatabase");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {
			// set up the environment so can grab these;
			// actually is;
			final File mgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File params = TestingUtilities.getTempFileFromResource("/sequestNotExistingDatabase.params", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final Mgf2SequestCaller m = new Mgf2SequestCaller();
			m.setSequestExe(getSequestExe());
			m.setHostsFile(getHostsFile());

			LOGGER.debug("outputDir=" + outputDir.getAbsolutePath());
			try {
				m.callSequest(new File(outputDir, "mytar.tar"), params, mgf, 10 * 1000, 10 * 1000, hdrFile);
			} catch (MprcException me) {
				LOGGER.debug("exception=" + me.getMessage());
				final String detailedMessage = MprcException.getDetailedMessage(me);
				Assert.assertTrue(detailedMessage.contains("database"), "error should indicate missing database");
				return;
			}
			Assert.fail("no error indicating missing database");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	private static final Pattern path_pattern = Pattern.compile("[$]PATH");

	private static String getSequestHungExecutable() {
		if (FileUtilities.isWindowsPlatform()) {
			return SEQUEST_HUNG_WINDOWS;
		} else {
			return SEQUEST_HUNG_LINUX;
		}
	}

	/**
	 * in this case the the mgf file has two peaklists;
	 * In this case will use a simulator for sequest. It will trigger a hung condition;
	 */
	@Test(enabled = true, groups = {"sequest"})
	public void testMGF2SequestScriptSequestHungCase() throws IOException {
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {

			// move the hung sequest resource to the temporary folder;
			final File cmd = TestingUtilities.getTempFileFromResource("/" + getSequestHungExecutable(), true, /*useSystemTempFolder*/ null);
			FileUtilities.copyFile(cmd, new File(outputDir, getSequestHungExecutable()), false);

			final File cmdfile = new File(outputDir, getSequestHungExecutable());

			// in the commandfile replace the $PATH
			final List<String> lines = new ArrayList<String>();

			final List<String> inputLines = FileUtilities.readLines(cmdfile);
			final String quotedOutputDir = Matcher.quoteReplacement(outputDir.getAbsolutePath());
			for (String line : inputLines) {
				final Matcher k = path_pattern.matcher(line);
				line = k.replaceAll(quotedOutputDir);
				lines.add(line);
			}
			FileUtilities.quietDelete(cmdfile);
			FileUtilities.writeStringsToFileNoBackup(cmdfile, lines, "\n");

			makeFileExecutable(cmdfile);

			// set up the environment so can grab these;
			// actually is;
			final File fmgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final File fparams = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);

			final Mgf2SequestCaller m = new Mgf2SequestCaller();
			m.setSequestExe(new File(outputDir, getSequestHungExecutable()).getAbsolutePath());
			m.setHostsFile(getHostsFile());

			final long startTime = System.currentTimeMillis();
			try {
				m.callSequest(new File(outputDir, "mytar.tar"), fparams, fmgf, 2 * 1000, 2 * 1000, hdrFile);
			} catch (MprcException me) {
				LOGGER.debug("exception=" + me.getMessage());
				final String message = MprcException.getDetailedMessage(me);
				Assert.assertTrue(message.contains("hung"), "error should indicate is hung");

				final long endTime = System.currentTimeMillis();
				if (endTime - startTime > 10 * 1000) {
					Assert.fail("The process was hung for too long, expected max 4 seconds, was " + (endTime - startTime) / 1000);
				}

				return;
			}
			Assert.fail("no error indicating hung sequest");
		} finally {
			TestingUtilities.quietDelete(folderName);
		}

	}

	private static void makeFileExecutable(final File f) {
		if (FileUtilities.isWindowsPlatform()) {
			// On windows there is no chmod
			return;
		}
		final ProcessBuilder builder = new ProcessBuilder(Arrays.asList("chmod", "766", f.getAbsolutePath()));
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.run();
	}

	/**
	 * in this case the the mgf file has two peaklists;
	 * the params file does not have a real database so a failure should occur;
	 */
	@Test(enabled = true, groups = {"sequest"})
	public void testMGF2SequestScriptSequestNoParams() {
		LOGGER.debug("running testMGF2SequestScriptSequestNoParams");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {

			// set up the environment so can grab these;
			// actually is;
			final File mgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);


			final File paramsFile = new File(outputDir, "noparams.params");


			final Mgf2SequestCaller m = new Mgf2SequestCaller();
			m.setHostsFile(getHostsFile());

			try {
				m.callSequest(new File(outputDir, "mytar.tar"), paramsFile, mgf, 10 * 1000, 10 * 1000, hdrFile);
			} catch (MprcException me) {
				LOGGER.debug("exception=" + me.getMessage());
				final String message = MprcException.getDetailedMessage(me);
				Assert.assertTrue(message.contains(paramsFile + " not found"), "error should indicate missing params file");
				return;
			}
			Assert.fail("no error indicating missing params file");

		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}


	}

	@Test(enabled = false, groups = {"fast", "linux", "sequest"})
	public void testLocalBigMgf() {
		// local mgf file;
		LOGGER.debug("running testLocalBigMgf");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {
			LOGGER.debug("tempfolder is at " + outputDir.getAbsolutePath());
			// set up the environment so can grab these;
			// actually is;
			final File mgf = TestingUtilities.getTempFileFromResource("/manysections.mgf", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final File params = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);

			final Mgf2SequestCallerStubbed m = new Mgf2SequestCallerStubbed();
			m.setHostsFile(getHostsFile());

			m.callSequest(new File(outputDir, "tarfile.tar"), params, mgf, 10 * 1000, 10 * 1000, hdrFile);

			// mgf file has 150 sections so the tar file should also;
			// tar name is mytar.tar;
			final TarWriter t = new TarWriter(new File(outputDir, "mytar.tar"));
			// now read number of sections;
			final int numheaders = TarReader.readNumberHeaders(t.getTarFile());
			//Assert.assertEquals (300, numheaders, "number of headers not correct");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	@Test(enabled = false, groups = {"fast", "sequest"})
	public void testLocalTwoSectionsMgfAllowLongCommandLine() {
		// local mgf file;
		LOGGER.debug("running testLocalTwoSectionsMgfAllowLongCommandLine");
		final File outputDir = FileUtilities.createTempFolder();
		final String folderName = outputDir.getAbsolutePath();
		try {
			LOGGER.debug("tempfolder is at " + outputDir.getAbsolutePath());
			// set up the environment so can grab these;
			// actually is;
			final File fmgf = TestingUtilities.getTempFileFromResource("/twosections.mgf", true, /*useSystemTempFolder*/ null);

			final File hdrFile = File.createTempFile("hdr", "db", outputDir);

			final File fparams = TestingUtilities.getTempFileFromResource("/mgftosequesttestSequestParams.params", true, /*useSystemTempFolder*/ null);

			final Mgf2SequestCallerStubbed m = new Mgf2SequestCallerStubbed();
			m.setHostsFile(getHostsFile());

			m.setMaxCommandLineLength(10000);

			m.callSequest(new File(outputDir, "tarfile.tar"), fparams, fmgf, 10 * 1000, 10 * 1000, hdrFile);

			// mgf file has 150 sections so the tar file should also;
			// tar name is mytar.tar;
			final TarWriter t = new TarWriter(new File(outputDir, "mytar.tar"));
			// now read number of sections;
			final int numheaders = TarReader.readNumberHeaders(t.getTarFile());
			//Assert.assertEquals (300, numheaders, "number of headers not correct");
		} catch (Exception t) {
			Assert.fail("exception occurred", t);
		} finally {
			TestingUtilities.quietDelete(folderName);
		}
	}

	/**
	 * used to test "sequest continous watchdog"
	 * give sequestCaller a working directory that does not correspond to the location of the dta files;
	 */


}


