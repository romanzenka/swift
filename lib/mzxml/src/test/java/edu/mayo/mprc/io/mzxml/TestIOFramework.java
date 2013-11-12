package edu.mayo.mprc.io.mzxml;


import edu.mayo.mprc.utilities.FileUtilities;
import org.proteomecommons.io.mzxml.v2_1.DataProcessing;
import org.proteomecommons.io.mzxml.v2_1.MsRun;
import org.proteomecommons.io.mzxml.v2_1.MzXMLPeakList;
import org.proteomecommons.io.mzxml.v2_1.MzXMLPeakListWriter;
import org.proteomecommons.io.raw.RawPeak;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public final class TestIOFramework {

	@Test(enabled = true, groups = {"fast", "integration"})
	public void testMzXMLWriter() {
		final File tempfolder = FileUtilities.createTempFolder();
		final File mzXmlFile = new File(tempfolder, "peaklist.mzXML");
		try {

			final MzXMLPeakListWriter w = new MzXMLPeakListWriter(mzXmlFile.getAbsolutePath());

			final RawPeak p1 = new RawPeak();


			p1.setCharge(2);
			p1.setIntensity(10102.0);
			p1.setMassOverCharge(1000.0);

			final MzXMLPeakList plist;

			plist = new MzXMLPeakList();
			plist.setBasePeakIntensity("" + p1.getIntensity());
			plist.setBasePeakMz("" + p1.getMassOverCharge());
			// scan number, note that these must start at 1 and be sequential
			plist.setNum("1");
			plist.setPrecursorScanNum("6328");
			plist.setMsLevel("2");
			plist.setScanType("Full");

			// msms ion peaks
			plist.setPeaks(new RawPeak[]{p1});

			w.setUse32BitPrecision(true);

			final MsRun msrun = new MsRun();
			msrun.setScanCount("1");

			plist.setMsRun(msrun);

			// need to add DataProcessing
			final DataProcessing dataProc = new DataProcessing();
			dataProc.setCentroided("1");

			msrun.addDataProcessing(dataProc);

			final MzXMLPeakList plist1;
			final RawPeak p2 = new RawPeak();

			// the precursor ion
			//p1.setCentroided(RawPeak.CENTROIDED);
			//p1.setMonoisotopic(RawPeak.MONOISOTOPIC);
			p2.setCharge(2);
			p2.setIntensity(10302.0);
			p2.setMassOverCharge(1003.0);

			plist1 = new MzXMLPeakList();
			plist1.setBasePeakIntensity("" + p2.getIntensity());
			plist1.setBasePeakMz("" + p2.getMassOverCharge());
			// scan number, note that these must start at 1 and be sequential
			plist1.setNum("2");
			plist1.setPrecursorScanNum("6328");
			plist1.setMsLevel("2");
			plist1.setScanType("Full");

			// msms ion peaks
			plist1.setPeaks(new RawPeak[]{p2});


			w.write(plist);

			// add a second list
			w.write(plist1);


			w.close();

		} catch (Exception t) {
			Assert.fail("exception in IOFramework", t);
		}
		FileUtilities.cleanupTempFile(tempfolder);
	}
}
