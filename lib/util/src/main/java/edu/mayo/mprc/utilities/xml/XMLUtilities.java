package edu.mayo.mprc.utilities.xml;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

public final class XMLUtilities {
	public static final String XML_START = "<?xml version=\"1.0\"?>";

	private XMLUtilities() {
	}

	public static StringBuilder appendline(final StringBuilder in, final String text) {
		return in.append(text).append("\r\n");
	}

	public static StringBuilder append(final StringBuilder in, final String text) {
		return in.append(text);
	}


	/**
	 * get XML text for name and value of attribute. The value's < and > gets escaped (ampersands do not, for compatibility reasons).
	 *
	 * @param name
	 * @param value
	 * @deprecated Thou shall not produce XML by hand.
	 */
	public static String wrapatt(final String name, final String value) {
		final String escapedValue = value.replace(">", "&gt;").replace("<", "&lt;");
		return " " + name + "=" + "\"" + escapedValue + "\"";
	}

	public static final String INDENT_TRANSFORM_STRING = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
			"\t<xsl:output method=\"xml\"/>\n" +
			"\t<xsl:param name=\"indent-increment\" select=\"'   '\"/>\n" +
			"\n" +
			"\t<xsl:template match=\"*\">\n" +
			"\t\t<xsl:param name=\"indent\" select=\"'&#xA;'\"/>\n" +
			"\n" +
			"\t\t<xsl:value-of select=\"$indent\"/>\n" +
			"\t\t<xsl:copy>\n" +
			"\t\t\t<xsl:copy-of select=\"@*\"/>\n" +
			"\t\t\t<xsl:apply-templates>\n" +
			"\t\t\t\t<xsl:with-param name=\"indent\"\n" +
			"\t\t\t\t                select=\"concat($indent, $indent-increment)\"/>\n" +
			"\t\t\t</xsl:apply-templates>\n" +
			"\t\t\t<xsl:if test=\"*\">\n" +
			"\t\t\t\t<xsl:value-of select=\"$indent\"/>\n" +
			"\t\t\t</xsl:if>\n" +
			"\t\t</xsl:copy>\n" +
			"\t</xsl:template>\n" +
			"\n" +
			"\t<xsl:template match=\"comment()|processing-instruction()\">\n" +
			"\t\t<xsl:copy/>\n" +
			"\t</xsl:template>\n" +
			"\n" +
			"\t<!-- WARNING: this is dangerous. Handle with care -->\n" +
			"\t<xsl:template match=\"text()[normalize-space(.)='']\"/>\n" +
			"\n" +
			"</xsl:stylesheet>";

	public static void indentXML(final Reader input, final Writer output) throws TransformerException {
		final TransformerFactory tfactory = TransformerFactory.newInstance();

		final Source tSource = new StreamSource(new StringReader(INDENT_TRANSFORM_STRING));

		final Transformer transformer = tfactory.newTransformer(tSource);
		final Source xml = new StreamSource(input);
		final Result result = new StreamResult(output);
		transformer.transform(xml, result);

		try {
			output.flush();
		} catch (IOException e) {
			LOGGER.debug("Could not flush the output.", e);
		}
	}

	private static Document loadDocument(final InputSource source) {
		Document doc = null;
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;

		try {
			builder = factory.newDocumentBuilder();
			try {
				doc = builder.parse(source);
			} catch (SAXException e) {
				throw new MprcException("error parsing xml document", e);
			} catch (IOException e) {
				throw new MprcException("error parsing xml document", e);
			}
		} catch (ParserConfigurationException e) {
			throw new MprcException("Unable to create builder", e);
		}
		return doc;
	}

	public static Document loadDocument(final Reader document) {
		try {
			final InputSource source = new InputSource(document);
			return loadDocument(source);
		} finally {
			FileUtilities.closeQuietly(document);
		}
	}

	public static Document loadDocument(final File document) {
		final InputSource source = new InputSource();
		source.setPublicId(document.getAbsolutePath());
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(document);
			source.setByteStream(stream);
			return loadDocument(source);
		} catch (FileNotFoundException e) {
			throw new MprcException("Could not load XML document from " + document.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(XMLUtilities.class);

	/**
	 * returns the text value associated with the element
	 *
	 * @param target - the element
	 * @return - the text value
	 */
	public static String getElementValue(final Element target) {

		final NodeList nodeList = target.getChildNodes();
		if (nodeList == null) {
			return null;
		}
		for (int current = 0; current < nodeList.getLength(); current++) {
			final Node node = nodeList.item(current);
			if (node instanceof Text) {
				final Text text = (Text) node;
				final String value = text.getNodeValue();
				if ((value != null) && (!value.isEmpty())) {
					return value;
				}
			}
		}
		return "";
	}


	/**
	 * find element with tagName whose id tag is 'idTagName' and id value is 'id_value'
	 *
	 * @param root
	 */
	public static Element findElement(final String idValue, final String idTagName, final String tagName, final Element root) {
		String textVal = null;
		final NodeList nl = root.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0) {
			// iterate over these
			for (int i = 0; i < nl.getLength(); i++) {
				final Element ei = (Element) nl.item(i);
				final NodeList n2 = ei.getElementsByTagName(idTagName);
				if (n2 != null && n2.getLength() > 0) {
					for (int j = 0; j < n2.getLength(); j++) {
						textVal = getElementValue((Element) n2.item(j));
						if (textVal.equals(idValue)) {
							return ei;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * find the unique element, returns null if not found or if there is more than one
	 *
	 * @param tagName - tag name
	 * @param root    - where to start looking
	 * @return the element
	 */
	public static Element findElement(final String tagName, final Element root) {
		if (root.getTagName().equals(tagName)) {
			return root;
		}
		final NodeList nl = root.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() == 1) {
			return (Element) nl.item(0);
		}
		return null;
	}

	/**
	 * replace the text value in the target
	 *
	 * @param target
	 * @param value
	 * @return
	 */
	public static boolean replaceTextValue(final Element target, final String value) {
		if (target == null) {
			return false;
		}
		final NodeList nodeList = target.getChildNodes();
		if (nodeList == null) {
			return false;
		}
		for (int current = 0; current < nodeList.getLength(); current++) {
			final Node node = nodeList.item(current);
			if (node instanceof Text) {
				final Text text = (Text) node;
				text.setData(value);
				return true;
			}
		}
		return false;
	}

	public static boolean setAttributeValue(final Element target, final String attributeName, final String value) {
		final NamedNodeMap map = target.getAttributes();
		final Node attributeValueHolder = map.getNamedItem(attributeName);
		if (attributeValueHolder != null) {
			attributeValueHolder.setNodeValue(value);
			return true;
		}
		return false;
	}

}
