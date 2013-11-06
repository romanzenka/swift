package edu.mayo.mprc.omssa;

import edu.mayo.mprc.utilities.xml.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

/**
 * this is used to read a usermods.xml file
 */
final class OmssaModsReader {
	private File userModsFile;
	private Document xmlDoc;

	/**
	 * Null Constructor
	 */
	OmssaModsReader(final File userModsFile) {
		this.userModsFile = userModsFile;
	}


	public void init() {
		if (xmlDoc == null) {
			xmlDoc = loadUserMods();
		}
	}

	/**
	 * load the template document so can transform it
	 *
	 * @return
	 */
	private Document loadUserMods() {
		return XMLUtilities.loadDocument(userModsFile);

	}

	public NodeList getMSModSpec() {
		init();
		return xmlDoc.getElementsByTagName("MSModSpec");
	}

	public Element getMSModSpecSet() {
		init();
		final NodeList nl = xmlDoc.getElementsByTagName("MSModSpecSet");
		if (nl != null && nl.getLength() > 0) {
			return (Element) nl.item(0);
		}
		return null;
	}


}
