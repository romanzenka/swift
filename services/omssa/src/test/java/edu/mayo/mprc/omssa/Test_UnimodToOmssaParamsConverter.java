package edu.mayo.mprc.omssa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.unimod.MockUnimodDao;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import edu.mayo.mprc.utilities.xml.XMLUtilities;
import org.apache.log4j.Logger;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Test(sequential = true)
public final class Test_UnimodToOmssaParamsConverter {

	private UnimodDao unimodDao;

	@BeforeClass
	public void setup() {
		unimodDao = new MockUnimodDao();
	}

	@Test(enabled = true)
	public void basictest_convertUnimodToOmssa() throws IOException, SAXException, TransformerException {

		/**
		 * ommssa only supports 30 user defined modifications so that is what we will support.
		 */
		final UnimodWriter w = new UnimodWriter(true /* include fixed */, true /* include variable */);
		w.write();

		final StringWriter formatedWriter = new StringWriter();
		XMLUtilities.indentXML(new StringReader(w.omssaWriter.toString()), formatedWriter);

		final String text = formatedWriter.toString();

		Assert.assertTrue(text.length() > 16000);

		//now round trip to test convertUnimodToOmssa

		final Set<ModSpecificity> matchedModSpecificities = this.findMatchedSpecificities(w.converter, w.umodSet, w.elemRoot.getElementsByTagName("MSModSpec"));

		Assert.assertEquals(matchedModSpecificities.size(), 28, "Unexpected number of mod specificities");


	}

	private Set<ModSpecificity> findMatchedSpecificities(final UnimodOmssaConverter converter, final Unimod umodSet, final NodeList msModSpecs) {
		final Set<ModSpecificity> matchedModSpecificities = new HashSet<ModSpecificity>();
		final Set<Element> unmatchedSpecs = new HashSet<Element>();


		for (int i = 0; i < msModSpecs.getLength(); i++) {
			final Element mod = (Element) msModSpecs.item(i);
			try {
				final ModSpecificity spec = converter.convertToModSpecificity(mod, umodSet);
				matchedModSpecificities.add(spec);
			} catch (Exception e) {
				unmatchedSpecs.add(mod);
				LOGGER.error(e);
			}
		}
		return matchedModSpecificities;
	}

	@Test(enabled = true)
	public void testWriteToSeparateUserModsFileFixedAndVariable() {
		runTestonUmimodsFileWrite(true /* include fixed */, true /* include variable */, 10 + 18);
	}

	@Test(enabled = true)
	public void testWriteToSeparateUserModsFileVariable() {
		runTestonUmimodsFileWrite(false /* include fixed */, true /* include variable */, 0 + 18);
	}

	@Test(enabled = true)
	public void testWriteToSeparateUserModsFileFixed() {
		runTestonUmimodsFileWrite(true /* include fixed */, false /* include variable */, 10 + 0);
	}

	private void runTestonUmimodsFileWrite(final boolean includeFixed, final boolean includeVariable, final int expectedSize) {

		final UnimodWriter w = new UnimodWriter(includeFixed, includeVariable);
		w.write();


		final File folder = FileUtilities.createTempFolder();

		String omssaParamsFileName = null;
		try {
			omssaParamsFileName = TestingUtilities.getUniqueTempFile(true, folder, ".tmp").getAbsolutePath();
		} catch (IOException e) {
			Assert.fail("failure opening new file", e);
		}
		w.writePersistent(omssaParamsFileName);
		final OmssaUserModsWriter omssaUserModsWriter = new OmssaUserModsWriter();
		String finalUserModsFileName = null;
		try {
			finalUserModsFileName = TestingUtilities.getUniqueTempFile(true, folder, ".tmp").getAbsolutePath();
		} catch (IOException e) {
			Assert.fail("failure opening new file", e);
		}
		omssaUserModsWriter.generateRuntimeUserModsFile(new File(finalUserModsFileName), new File(omssaParamsFileName));
		// need to look in the finalUserModsFileName to see if the modifications are there...
		final OmssaModsReader r = new OmssaModsReader(new File(finalUserModsFileName));
		final NodeList msModSpecs = r.getMSModSpec();
		// convert these nodes to Specificity's
		final Set<ModSpecificity> matchedModSpecificities = findMatchedSpecificities(w.converter, w.umodSet, msModSpecs);

		Assert.assertTrue(matchedModSpecificities.size() == expectedSize, "actual size=" + matchedModSpecificities.size());

	}

	@Test(enabled = true)
	public void testWithPreConfiguredParamsFile() {
		final File folder = FileUtilities.createTempFolder();

		File omssaParamsFile = null;
		try {
			omssaParamsFile = TestingUtilities.getTempFileFromResource("/omssa.params.xml", true, folder);
		} catch (IOException e) {
			Assert.fail("failure opening new file", e);
		}
		final OmssaUserModsWriter o = new OmssaUserModsWriter();
		File finalUserModsFile = null;
		try {
			finalUserModsFile = TestingUtilities.getUniqueTempFile(true, folder, ".tmp");
		} catch (IOException e) {
			Assert.fail("failure opening new file", e);
		}
		o.generateRuntimeUserModsFile(finalUserModsFile, omssaParamsFile);
		// need to look in the finalUserModsFileName to see if the modifications are there...
		final OmssaModsReader r = new OmssaModsReader(finalUserModsFile);
		final NodeList msModSpecs = r.getMSModSpec();
		// convert these nodes to Specificity's
//		Set<ModSpecificity> matchedSpecificities = findMatchedSpecificities(w.converter, w.umodSet, msModSpecs);
//
//		Assert.assertTrue(matchedSpecificities.size() == msModSpecs.getLength(), "actual size=" + matchedSpecificities.size());
	}


	class UnimodWriter {

		public final Element elemRoot;
		public Document xmldoc;
		public StringWriter omssaWriter;
		public final UnimodOmssaConverter converter;
		public final Unimod umodSet;

		public UnimodWriter(final boolean includeFixed, final boolean includeVariable) {
			/**
			 * ommssa only supports 30 user defined modifications so that is what we will support.
			 */
			final List<ModSpecificity> fixedList = new ArrayList<ModSpecificity>();
			final List<ModSpecificity> varList = new ArrayList<ModSpecificity>();
			umodSet = Test_UnimodToOmssaParamsConverter.getFixedandVariableSpecificities(fixedList, varList, includeFixed, includeVariable);

			final ModsUtilities modsUtilities = new ModsUtilities();
			converter = new UnimodOmssaConverter(modsUtilities);

			xmldoc = new DocumentImpl(true);

			elemRoot = xmldoc.createElement("MSSearchSettings");
			xmldoc.appendChild(elemRoot);

			converter.convertUnimodToOmssa(false, varList, xmldoc);
			converter.convertUnimodToOmssa(true, fixedList, xmldoc);


		}

		public void write() {
			omssaWriter = new StringWriter();
			final XMLSerializer serializer = new XMLSerializer(omssaWriter, new OutputFormat(xmldoc));
			try {
				serializer.serialize(xmldoc);
			} catch (IOException e) {
				throw new MprcException("unable to serialize document", e);
			}
		}

		public void writePersistent(final String filename) {
// Use a Transformer for output
			final TransformerFactory tFactory =
					TransformerFactory.newInstance();
			Transformer transformer = null;
			try {
				transformer = tFactory.newTransformer();
			} catch (TransformerConfigurationException e) {
				throw new MprcException(e);
			}

			final DOMSource source = new DOMSource(xmldoc);
			FileOutputStream o = null;
			try {
				o = new FileOutputStream(filename);
			} catch (FileNotFoundException e) {
				throw new MprcException("could not open output stream on " + filename);
			}
			final StreamResult result = new StreamResult(o);
			try {
				transformer.transform(source, result);
			} catch (TransformerException e) {
				throw new MprcException("failure writing out the user mods file");
			}


		}
	}

	/**
	 * gather fixed and variable mod specificiies
	 *
	 * @param fixedModsList - add fixed ones here
	 * @param varModsList   - add variable ones here
	 * @param fixed         - if should include the fixed mods
	 * @param variable      - if should include the variable mods
	 * @return
	 */
	private static Unimod getFixedandVariableSpecificities(final List<ModSpecificity> fixedModsList, final List<ModSpecificity> varModsList, final boolean fixed, final boolean variable) {

		final InputStream umodStream = new Test_UnimodToOmssaParamsConverter().getClass().getResourceAsStream("/edu/mayo/mprc/unimod/unimod.xml");
		final Unimod umodSet = new Unimod();
		umodSet.parseUnimodXML(umodStream);

		/**
		 * ommssa only supports 30 user defined modifications so that is what we will support.
		 */
		if (fixed) {
			fixedModsList.addAll(new ArrayList<ModSpecificity>(umodSet.getAllSpecificities(false)).subList(0, 10));
		}
		if (variable) {
			varModsList.addAll(new ArrayList<ModSpecificity>(umodSet.getAllSpecificities(false)).subList(10, 28));
		}
		return umodSet;
	}

	private static final Logger LOGGER = Logger.getLogger(Test_UnimodToOmssaParamsConverter.class);

	@Test(enabled = true)
	/**
	 * will be searching in
	 * <MSModSpecSet
	 xmlns="http://www.ncbi.nlm.nih.gov"
	 xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
	 xs:schemaLocation="http://www.ncbi.nlm.nih.gov OMSSA.xsd"
	 >
	 <MSModSpec>
	 <MSModSpec_mod>
	 <MSMod value="usermod1">119</MSMod>
	 </MSModSpec_mod>
	 <MSModSpec_type>
	 <MSModType value="modaa">0</MSModType>
	 </MSModSpec_type>
	 */
	public void testFindElement() {
		final File folder = FileUtilities.createTempFolder();
		final String searchId = "157";
		File templateFile = null;
		try {
			templateFile = TestingUtilities.getTempFileFromResource("/usermods.xml", true, folder);
		} catch (IOException e) {
			Assert.fail("failed opening temporary file on resource usermods.xml", e);
		}
		final OmssaModsReader r = new OmssaModsReader(templateFile);
		final Element msModSpecSet = r.getMSModSpecSet();
		final Element el = XMLUtilities.findElement(searchId, "MSMod", "MSModSpec", msModSpecSet);
		Assert.assertNotNull(el);
		Assert.assertTrue(el.getTagName().equals("MSModSpec"), "top tag should be MSModSpec, not " + el.getTagName());
		// now find the id value
		final Element mod = XMLUtilities.findElement("MSMod", el);
		final String id = XMLUtilities.getElementValue(mod);
		Assert.assertEquals(searchId, id, "id mismatch");

		// now test replace
		XMLUtilities.replaceTextValue(mod, "1");
		final String id1 = XMLUtilities.getElementValue(mod);
		LOGGER.debug("id1=" + id1);
		Assert.assertEquals(id1, "1", "id not set to 1");

	}
}
