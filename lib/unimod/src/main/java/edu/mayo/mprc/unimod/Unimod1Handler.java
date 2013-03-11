package edu.mayo.mprc.unimod;

import com.google.common.base.Splitter;
import edu.mayo.mprc.MprcException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parser for unimod.xml file in the Scaffold format (older version of unimod.xml).
 * <p/>
 * There is a particular caveat with this older format regarding composition formulas. See {@link #convertComposition}.
 *
 * @author Roman Zenka
 */
class Unimod1Handler extends DefaultHandler {
	private final Unimod into;

	private final Map<Integer, ModBuilder> modBuilders = new HashMap<Integer, ModBuilder>(300);
	private final Map<Integer, String> positions = new HashMap<Integer, String>(5);
	private final Map<Integer, String> classifications = new HashMap<Integer, String>(5);

	private static final Pattern COMPOSITION_CONVERT = Pattern.compile("^(.+?)(\\d+)(\\([1-9-][0-9]*\\)$|$)");
	// Flip the number from before the element to the end of its name.
	private static final String ELEMENT_NUMBER_SWAP = "$2$1$3";

	private enum NotesState {
		NO_NOTES,
		MODIFICATIONS_ROW,
		SPECIFICITY_ROW
	}

	private NotesState notesState = NotesState.NO_NOTES;

	private final StringBuilder miscNotes = new StringBuilder(100);
	private ModBuilder currentMod;
	private SpecificityBuilder currentSpecificity;

	public Unimod1Handler(final Unimod into) {
		this.into = into;
	}

	@Override
	public void startElement(final String namespaceURI, final String localName, final String qualifiedName, final Attributes attr) {
		if ("unimod".equals(localName)) {
			into.setMajorVersion(attr.getValue("majorVersion"));
			into.setMinorVersion(attr.getValue("minorVersion"));
		} else if ("modifications_row".equals(localName)) {
			/*
			Example

			<modifications_row

			username_of_poster="unimod"
			avge_mass="47.9982"
			mono_mass="47.984744"
			record_id="345"
			full_name="cysteine oxidation to cysteic acid"
			code_name="Cysteic_acid"
			date_time_modified="2005-10-03 00:21:40"
			date_time_posted="2005-09-10 22:39:38"
			composition="O18(1)"
			group_of_poster="admin">
					  <misc_notes></misc_notes>
					 */

			notesState = NotesState.MODIFICATIONS_ROW;
			final Integer modId = UnimodHandler.getIntegerValue(attr, "record_id");
			currentMod = getModBuilder(modId);
			currentMod.setMassAverage(UnimodHandler.getDoubleValue(attr, "avge_mass"));
			currentMod.setMassMono(UnimodHandler.getDoubleValue(attr, "mono_mass"));
			currentMod.setFullName(attr.getValue("", "full_name"));
			currentMod.setTitle(attr.getValue("", "code_name"));
			final String convertedComposition = convertComposition(attr.getValue("", "composition"));
			currentMod.setComposition(convertedComposition);
			modBuilders.put(modId, currentMod);
		} else if ("alt_names_row".equals(localName)) {
			// <alt_names_row alt_name="Pierce EZ-Link PEO-Iodoacetyl Biotin" record_id="321" mod_key="20"></alt_names_row>
			final Integer modId = UnimodHandler.getIntegerValue(attr, "mod_key");
			final ModBuilder modBuilder = getModBuilder(modId);
			modBuilder.getAltNames().add(attr.getValue("", "alt_name"));
		} else if ("positions_row".equals(localName)) {
			//  <positions_row position="-" record_id="1"></positions_row>
			//  <positions_row position="Anywhere" record_id="2"></positions_row>
			positions.put(
					UnimodHandler.getIntegerValue(attr, "record_id"),
					attr.getValue("position"));
		} else if ("classifications_row".equals(localName)) {
			//     <classifications_row record_id="2" classification="Post-translational"></classifications_row>
			classifications.put(
					UnimodHandler.getIntegerValue(attr, "record_id"),
					attr.getValue("classification"));
		} else if ("specificity_row".equals(localName)) {
			notesState = NotesState.SPECIFICITY_ROW;
			// <specificity_row position_key="3" nl_mono_mass="0.000000" classifications_key="13" record_id="2487" one_letter="N-term" mod_key="1" nl_composition="" hidden="0" nl_avge_mass="0.0000" spec_group="2">
			// <misc_notes></misc_notes>
			// </specificity_row>
			currentMod = getModBuilder(UnimodHandler.getIntegerValue(attr, "mod_key"));
			final Boolean hidden = UnimodHandler.getBooleanValue(attr, "hidden");
			final String oneLetter = attr.getValue("", "one_letter");
			final Integer specificityGroup = UnimodHandler.getIntegerValue(attr, "spec_group");
			final String classification = classifications.get(UnimodHandler.getIntegerValue(attr, "classifications_key"));
			final String position = positions.get(UnimodHandler.getIntegerValue(attr, "position_key"));
			currentSpecificity = currentMod.addSpecificityFromUnimod(oneLetter, position, hidden, classification, specificityGroup);
		} else if ("misc_notes".equals(localName)) {
			miscNotes.setLength(0);
		}
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		if (notesState != NotesState.NO_NOTES) {
			miscNotes.append(ch, start, length);
		}
	}

	@Override
	public void endElement(final String namespaceURI, final String localName, final String qualifiedName) {
		if ("unimod".equals(localName)) {
			dump();
		} else if ("misc_notes".equals(localName)) {
			switch (notesState) {
				case MODIFICATIONS_ROW:
					// Do nothing, comments not supported for mods
					break;
				case SPECIFICITY_ROW:
					currentSpecificity.setComments(miscNotes.toString());
					miscNotes.setLength(0);
					break;
				case NO_NOTES:
					// Do nothing
					break;
				default:
					throw new MprcException("Unknown state");
			}
		}
	}

	/**
	 * Unimod 1.0 provides the elemental compositions of modifications are provided in a different
	 * format. E.g. <sup>18</sup>O isotope is reported as {@code O18}, which has been changed in Unimod 2 to actual 18O.
	 * In order to be consistent, we upgrade to 2.0 by flipping the number after the text to the beginning of it.
	 */
	static String convertComposition(final String composition) {
		final Iterable<String> parts = Splitter.on(' ').trimResults().omitEmptyStrings().split(composition);
		final StringBuilder result = new StringBuilder(composition.length() + 1);
		for (final String part : parts) {
			result.append(' ');
			result.append(COMPOSITION_CONVERT.matcher(part).replaceFirst(ELEMENT_NUMBER_SWAP));
		}
		return result.length() > 0 ? result.substring(1) : "";
	}

	private ModBuilder getModBuilder(final int recordId) {
		final ModBuilder builder = modBuilders.get(recordId);
		if (builder == null) {
			final ModBuilder newBuilder = new ModBuilder();
			newBuilder.setRecordID(recordId);
			modBuilders.put(recordId, newBuilder);
			return newBuilder;
		}
		return builder;
	}

	private void dump() {
		for (final ModBuilder builder : modBuilders.values()) {
			into.add(builder.build());
		}
	}
}
