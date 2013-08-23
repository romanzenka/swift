package edu.mayo.mprc.omssa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Terminus;
import edu.mayo.mprc.unimod.Unimod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;


/**
 * This class can take the object graph represented with {@link edu.mayo.mprc.unimod.ModSpecificity} and convert
 * them into a XML element that can be inserted into the params file as MSSearchSettings.MSSearchSettings_usermods.
 * <p/>
 * The Modification class represents the Unimod entries (currently a subset of the features in Unimod) and we want to allow
 * the use of all of unimod modifications in an OMSSA search.
 */
final class UnimodOmssaConverter {
	private ModsUtilities modsUtilities;

	public UnimodOmssaConverter(final ModsUtilities modsUtilities) {
		this.modsUtilities = modsUtilities;
	}

	/**
	 * Takes a list of modifications and adds them to the xmldoc given.  This will first make sure that it is a valid
	 * OMSSA param set document and then will add to the correct place in the document.  You need to pass in two collections
	 * the one for fixed and another for variable.  If the same modification is in both then the fixed is discarded.
	 * <p/>
	 * When this method is done the xmldoc given will be modified.
	 */
	public void convertUnimodToOmssa(final boolean fixedMods, final Collection<ModSpecificity> mods, final Document xmldoc) {
		final Element elemMSSearchSettings = xmldoc.getDocumentElement();
		if (elemMSSearchSettings == null || elemMSSearchSettings.getNodeName() == null || !elemMSSearchSettings.getNodeName().equals("MSSearchSettings")) {
			throw new MprcException("Does not appear to be a valid OMSSA params file.  The root element needs to be named 'MSSearchSettings'.");
		}

		// Lets clear all the fixed/variable mods, keep only the other set
		final String elementToRetainName = !fixedMods ? "MSSearchSettings_fixed" : "MSSearchSettings_variable";
		Element listModsToRetain = (Element) getSingleElement(elemMSSearchSettings, elementToRetainName);
		if (listModsToRetain == null) {
			listModsToRetain = xmldoc.createElement(elementToRetainName);
			elemMSSearchSettings.appendChild(listModsToRetain);
		}
		final NodeList childNodes = listModsToRetain.getChildNodes();
		final Set<Integer> modIdsToRetain = new TreeSet<Integer>();
		// These two mods are always retained - defaults
		modIdsToRetain.add(119);
		modIdsToRetain.add(120);
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node child = childNodes.item(i);
			if ("MSMod".equalsIgnoreCase(child.getNodeName())) {
				final String valueString = child.getTextContent();
				final int value = Integer.parseInt(valueString);
				modIdsToRetain.add(value);
			}
		}

		final List<String> ids = new ArrayList<String>(mods.size());

		final NodeList matchingNodesUsermods = elemMSSearchSettings.getElementsByTagName("MSSearchSettings_usermods");

		final Element elemUsermods;
		if (matchingNodesUsermods.getLength() == 0) {
			elemUsermods = xmldoc.createElement("MSSearchSettings_usermods");
			elemMSSearchSettings.appendChild(elemUsermods);
		} else {
			elemUsermods = (Element) matchingNodesUsermods.item(0);
		}

		final NodeList matchingNodesMSModSpecSet = elemUsermods.getElementsByTagName("MSModSpecSet");
		final Element elemMSModSpecSet;
		if (matchingNodesMSModSpecSet.getLength() == 0) {
			elemMSModSpecSet = xmldoc.createElement("MSModSpecSet");
			elemUsermods.appendChild(elemMSModSpecSet);
		} else {
			elemMSModSpecSet = (Element) matchingNodesMSModSpecSet.item(0);
		}

		cleanupAllModsNotInList(elemMSModSpecSet, modIdsToRetain);

		final Queue<String> availableUserModIds = getAvailableIds(elemMSModSpecSet);

		if (mods.size() > availableUserModIds.size()) {
			throw new MprcException("OMSSA has only " + availableUserModIds.size() +
					" modifications left (after " + (fixedMods ? "variable" : "fixed") + " modifications are taken out) and you want additional " + mods.size() + " " + (fixedMods ? "fixed" : "variable") + " mods.");
		}

		for (final ModSpecificity spec : mods) {
			final Element elemMSModSpec = xmldoc.createElement("MSModSpec");
			elemMSModSpecSet.appendChild(elemMSModSpec);

			final Element elemMod = xmldoc.createElement("MSModSpec_mod");
			elemMSModSpec.appendChild(elemMod);

			final Element elemId = xmldoc.createElement("MSMod");
			final String id = availableUserModIds.poll();
			elemId.setAttribute("value", modsUtilities.getModValueLookup().get(id));

			//add to the list of ids that we need to add
			ids.add(id);

			elemId.setTextContent(id);

			elemMod.appendChild(elemId);

			final String modType = ModsUtilities.findModType(spec);
			final Element elemModType = xmldoc.createElement("MSModSpec_type");
			elemMSModSpec.appendChild(elemModType);

			final Element elemModTypeValue = xmldoc.createElement("MSModType");
			elemModType.appendChild(elemModTypeValue);
			elemModTypeValue.setAttribute("value", modType);
			elemModTypeValue.setTextContent(modsUtilities.getModTypeLookup().get(modType));

			final Element elemName = xmldoc.createElement("MSModSpec_name");
			elemName.setTextContent(spec.toString());
			elemMSModSpec.appendChild(elemName);

			if (spec.getModification().getMassMono() != null) {
				final Element elemMonoMass = xmldoc.createElement("MSModSpec_monomass");
				elemMSModSpec.appendChild(elemMonoMass);
				elemMonoMass.setTextContent(String.valueOf(spec.getModification().getMassMono()));
			}

			if (spec.getModification().getMassAverage() != null) {
				final Element elemAvgMass = xmldoc.createElement("MSModSpec_averagemass");
				elemMSModSpec.appendChild(elemAvgMass);
				elemAvgMass.setTextContent(String.valueOf(spec.getModification().getMassAverage()));
			}

			final Element elemN15Mass = xmldoc.createElement("MSModSpec_n15mass");
			elemN15Mass.setTextContent("0");
			elemMSModSpec.appendChild(elemN15Mass);


			if (modType.endsWith("aa")) {
				final Element elemModResidues = xmldoc.createElement("MSModSpec_residues");
				elemMSModSpec.appendChild(elemModResidues);

				final Element elemResidueE = xmldoc.createElement("MSModSpec_residues_E");
				elemResidueE.setTextContent(spec.getSite().toString());
				elemModResidues.appendChild(elemResidueE);
			}
		}

		//now we need to insert the list of mods that should be used.

		setSelectedMods((fixedMods ? "MSSearchSettings_fixed" : "MSSearchSettings_variable"), ids, elemMSSearchSettings, xmldoc);

	}

	private void cleanupAllModsNotInList(final Element elemMSModSpecSet, final Set<Integer> modIdsToRetain) {
		final NodeList childNodes = elemMSModSpecSet.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node child = childNodes.item(i);
			if ("MSModSpec".equals(child.getNodeName())) {
				//                 <MSModSpec_mod>
				// <MSMod value="usermod1">119</MSMod>
				final Node MSModSpecMod = getSingleElement((Element) child, "MSModSpec_mod");
				final Node MSMod = getSingleElement((Element) MSModSpecMod, "MSMod");
				final int id = Integer.parseInt(MSMod.getTextContent());
				if (!modIdsToRetain.contains(id)) {
					elemMSModSpecSet.removeChild(child);
				}
			}
		}
	}

	/**
	 * @return Return single element, throw if there are multiple, return null if there is none.
	 */
	private Node getSingleElement(final Element parent, final String name) {
		final NodeList elementsToRetain = parent.getElementsByTagName(name);
		if (elementsToRetain.getLength() > 1) {
			throw new MprcException("OMSSA params file is corrupted, multiple <" + name + "> element.");
		} else if (elementsToRetain.getLength() == 0) {
			return null;
		}
		return elementsToRetain.item(0);
	}

	/**
	 * Takes a list of modifications and adds them to the xmldoc given.  This will first make sure that it is a valid
	 * OMSSA param set document and then will add to the correct place in the document.  You need to pass in two collections
	 * the one for fixed and another for variable.
	 * <p/>
	 * When this method is done the xmldoc given will be modified.
	 */
	public void convertUnimodToOmssa(final boolean fixedMods, final Collection<ModSpecificity> mods, final Document xmldoc, final Map<ModSpecificity, String> ids) {
		final Element elemMSSearchSettings = xmldoc.getDocumentElement();
		if (elemMSSearchSettings == null || elemMSSearchSettings.getNodeName() == null || !elemMSSearchSettings.getNodeName().equals("MSSearchSettings")) {
			throw new MprcException("Does not appear to be a valid OMSSA params file.  The root element needs to be named 'MSSearchSettings'.");
		}

		final List<String> filteredIds = OmssaUserModsWriter.getIds(mods, ids);
		//now we need to insert the list of mods that should be used.

		setSelectedMods((fixedMods ? "MSSearchSettings_fixed" : "MSSearchSettings_variable"), filteredIds, elemMSSearchSettings, xmldoc);

	}

	/**
	 * Guest the queue of ids that can be used by omssa for user mods.  It takes the element for user modes and makes
	 * sure we don't use ones that are already taken within that element.
	 *
	 * @param elemMsModSpecSet
	 * @return
	 */
	protected Queue<String> getAvailableIds(final Element elemMsModSpecSet) {

		final int userModsSoFar = 0;

		final Queue<String> available = new LinkedList<String>();
		//need to start at 121 since we need room for 2 defaults
		for (int i = 119; i <= 128; i++) {
			available.add(String.valueOf(i));
		}
		for (int i = 142; i <= 161; i++) {
			available.add(String.valueOf(i));
		}

		//determine which are already taken
		final Set<String> alreadyTakenIds = new HashSet<String>();

		final NodeList existing = elemMsModSpecSet.getElementsByTagName("MSModSpec");
		for (int i = 0; i < existing.getLength(); i++) {
			final NodeList msModSpecMods = ((Element) existing.item(i)).getElementsByTagName("MSModSpec_mod");
			if (msModSpecMods.getLength() > 0) {
				final NodeList msMods = ((Element) msModSpecMods.item(0)).getElementsByTagName("MSMod");
				if (msMods.getLength() > 0) {
					final Element msMod = (Element) msMods.item(0);
					//add to set of taken ids
					final String content = msMod.getTextContent();
					if (content != null) {
						alreadyTakenIds.add(content.trim());
					}
				}
			}
		}

		available.removeAll(alreadyTakenIds);
		return available;
	}

	protected void setSelectedMods(final String elementName, final List<String> modIds, final Element parentNode, final Document xmldoc) {

		final NodeList modNodes = xmldoc.getElementsByTagName(elementName);
		final Element elemMods;
		if (modNodes.getLength() > 1) {
			throw new MprcException("There were multiple " + elementName + " tags in the xml document!");
		} else if (modNodes.getLength() > 0) {
			elemMods = (Element) modNodes.item(0);
			final NodeList existingMods = elemMods.getElementsByTagName("MSMod");
			for (int i = existingMods.getLength() - 1; i >= 0; i--) {
				elemMods.removeChild(existingMods.item(i));
			}
		} else {
			elemMods = xmldoc.createElement(elementName);
			parentNode.appendChild(elemMods);
		}

		for (final String id : modIds) {
			final Element elemMSMod = xmldoc.createElement("MSMod");
			elemMSMod.setAttribute("value", modsUtilities.getModValueLookup().get(id));
			elemMSMod.setTextContent(id);
			elemMods.appendChild(elemMSMod);
		}

	}

	/**
	 * match a MSModSpec element in the omssa.params.xml file with a Specificity from the set of modifications @link{#unimod}
	 *
	 * @param elemMSModSpec - tag section for tag MSModSpec
	 * @param unimod
	 * @return
	 */
	public ModSpecificity convertToModSpecificity(final Element elemMSModSpec, final Unimod unimod) {
		final double massShift = Double.valueOf(elemMSModSpec.getElementsByTagName("MSModSpec_monomass").item(0).getTextContent());
		final Element elemType = (Element) elemMSModSpec.getElementsByTagName("MSModSpec_type").item(0);
		final String modTypeEnum = elemType.getElementsByTagName("MSModType").item(0).getTextContent();

		//get the residue from the document, the residue is optional for us so we will need more bounds checking.
		final NodeList residues = elemMSModSpec.getElementsByTagName("MSModSpec_residues");
		String residue = null;
		if (residues.getLength() > 0) {
			if (residues.getLength() > 1) {
				throw new MprcException("Only a single residue is supported since we are working on the Specificity level of abstraction.");
			}
			residue = ((Element) residues.item(0)).getElementsByTagName("MSModSpec_residues_E").item(0).getTextContent();
		}

		//get the modtype in a more friendly format than the enumeration that appears to be the way OMSSA likes to communicate,
		//omssa does provide a "value" attribute but I think this was a new feature added later and not fully compatible way of
		//communicating the modType.
		final String modType = modsUtilities.getModTypeLookup().inverse().get(modTypeEnum);
		if (modType == null) {
			throw new MprcException("Could not find a modType for the enumerated value" + modTypeEnum);
		}

		final Character site = getSite(modType, residue);
		final Terminus terminus = getPosition(modType);
		final boolean proteinOnly = getProteinOnly(modType);

		final ModSpecificity spec = unimod.findSingleMatchingModificationSet(massShift, ModsUtilities.MOD_MASS_TOL, site, terminus, proteinOnly, /*hidden, null means don't consider*/ null);

		if (spec == null) {
			throw new MprcException("Can't find modification in Unimod with:" + massShift + "@" + site);
		}
		return spec;
	}

	/**
	 * modn, modnaa, modc, modcaa are protein only mods
	 */
	protected static boolean getProteinOnly(final String modType) {
		return modType.equals("modn") ||
				modType.equals("modnaa") ||
				modType.equals("modc") ||
				modType.equals("modcaa");
	}

	protected static Terminus getPosition(final String modType) {
		if (modType.startsWith("modc")) {
			return Terminus.Cterm;
		} else if (modType.startsWith("modn")) {
			return Terminus.Nterm;
		}
		return Terminus.Anywhere;
	}

	protected static Character getSite(final String modType, final String residue) {
		if (residue == null || residue.isEmpty()) {
			if (!modType.endsWith("aa")) {
				return '*';
			} else {
				throw new MprcException("Could not determine the site form modType " + modType + " when no residue is provided");
			}
		} else {
			return residue.charAt(0);
		}
	}


}
