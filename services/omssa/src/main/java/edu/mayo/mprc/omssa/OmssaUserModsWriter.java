package edu.mayo.mprc.omssa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.xml.XMLUtilities;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Takes OMSSA parameter file and extracts the user mods file to be fed to OMSSA separately.
 * <p/>
 * Since all the mods are already in the OMSSA param file, this activity seems very silly, however that is what
 * OMSSA seems to require.
 */
@Component("omssaUserModsWriter")
final class OmssaUserModsWriter {

	// Where to find the user mods element within the original document
	private static final String SOURCE_ELEMENT = "/MSSearchSettings/MSSearchSettings_usermods/MSModSpecSet";
	private static final String TARGET_ELEMENT = "/MSModSpecSet";

	private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();


	public OmssaUserModsWriter() {
	}

	/**
	 * First save the file using a transformer.
	 * Then re-indent the file so OMSSA actually likes it.
	 *
	 * @param document
	 * @param resultingFile
	 */
	private void writeRevisedModsDocument(final Document document, final File resultingFile) {
		// Use a Transformer for output
		final TransformerFactory tFactory =
				TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = tFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new MprcException(e);
		}

		final DOMSource source = new DOMSource(document);
		FileOutputStream o = null;
		final File tempFile = new File(resultingFile.getParentFile(), resultingFile.getName() + ".1");
		try {
			try {
				o = new FileOutputStream(tempFile);
			} catch (FileNotFoundException e) {
				throw new MprcException("could not open output stream on " + tempFile.getAbsolutePath(), e);
			}
			final StreamResult result = new StreamResult(o);
			try {
				transformer.transform(source, result);
			} catch (TransformerException e) {
				throw new MprcException("failure writing out the user mods file", e);
			}
		} finally {
			FileUtilities.closeQuietly(o);
		}

		// this may not be indented such that omssa can handle it

		BufferedReader r = null;
		FileWriter w = null;
		try {
			r = FileUtilities.getReader(tempFile);
			w = FileUtilities.getWriter(resultingFile);
			try {
				XMLUtilities.indentXML(r, w);
			} catch (TransformerException e) {
				throw new MprcException(e);
			}
		} finally {
			FileUtilities.closeQuietly(w);
			FileUtilities.closeQuietly(r);
		}

	}

	/**
	 * Extracts the user mods from the omssa parameter file and stores them in a separate usermods.xml file.
	 *
	 * @param usermodsFile    - modified user mods file - output, lists only the user mods portion of the original document
	 * @param omssaParamsFile - omssa params file - input, defines which mods to be used
	 */
	public void generateRuntimeUserModsFile(final File usermodsFile, final File omssaParamsFile) {
		final Reader reader = ResourceUtilities.getReader("classpath:usermods.xml", OmssaUserModsWriter.class);
		final Document template;
		final Node root;
		try {
			template = XMLUtilities.loadDocument(reader);
			root = template.getDocumentElement();
			removeAllChildren(root);

		} catch (Exception e) {
			throw new MprcException("Could not process the usermod.xml template", e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}

		try {
			final Document document = XMLUtilities.loadDocument(omssaParamsFile);
			final Node usermods = getNodeWithXPath(document, SOURCE_ELEMENT);
			final NodeList childNodes = usermods.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				final Node item = childNodes.item(i);
				final Node imported = template.importNode(item, true);
				root.appendChild(imported);
			}

		} catch (Exception e) {
			throw new MprcException("Could not parse the OMSSA params file " + omssaParamsFile.getAbsolutePath(), e);
		}

		writeRevisedModsDocument(template, usermodsFile);

	}

	private Node getNodeWithXPath(final Document document, final String xpath) throws XPathExpressionException {
		final XPath xPath = XPATH_FACTORY.newXPath();
		final XPathExpression xPathExpression = xPath.compile(xpath);
		return (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
	}

	private static void removeAllChildren(final Node root) {
		while (root.hasChildNodes()) {
			root.removeChild(root.getChildNodes().item(0));
		}
	}

	/**
	 * retrieve the ids associated with a group of mods
	 *
	 * @param mods - mods want to retrieve ids for
	 * @param ids  - variable and fixed mod ids
	 * @return ArrayList of ids as String
	 */
	public static List<String> getIds(final Collection<ModSpecificity> mods, final Map<ModSpecificity, String> ids) {
		final List<String> selected = new ArrayList<String>();
		for (final ModSpecificity spec : mods) {
			final String id = ids.get(spec);
			if (spec != null) {
				selected.add(id);
			}

		}
		return selected;
	}
}
