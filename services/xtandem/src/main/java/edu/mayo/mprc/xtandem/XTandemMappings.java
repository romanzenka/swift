package edu.mayo.mprc.xtandem;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.xml.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.regex.Pattern;

public final class XTandemMappings implements Mappings {
	public static final String DATABASE_TAXON = "database1";

	private static final String PEP_TOL_UNIT = "spectrum, parent monoisotopic mass error units";
	private static final String PEP_TOL_PLUS = "spectrum, parent monoisotopic mass error plus";
	private static final String PEP_TOL_MINUS = "spectrum, parent monoisotopic mass error minus";
	private static final String FRAG_TOL_UNIT = "spectrum, fragment monoisotopic mass error units";
	private static final String FRAG_TOL_VALUE = "spectrum, fragment monoisotopic mass error";
	private static final String VAR_MODS = "residue, potential modification mass";
	private static final String FIXED_MODS = "residue, modification mass";
	private static final String DATABASE = "protein, taxon";
	private static final String ENZYME = "protein, cleavage site";
	private static final String MISSED_CLEAVAGES = "scoring, maximum missed cleavage sites";

	private static final Pattern IONS_PATTERN = Pattern.compile("^scoring, (.*) ions");
	private static final String[] SUPPORTED_IONS = new String[]{"x", "y", "z", "a", "b", "c"};

	/**
	 * We recognize all following params plus anything that matches {@link #IONS_PATTERN}.
	 */
	private static final Set<String> PARSED_PARAMS = new HashSet<String>(Arrays.asList(PEP_TOL_PLUS,
			PEP_TOL_MINUS,
			PEP_TOL_UNIT,
			FRAG_TOL_VALUE,
			FRAG_TOL_UNIT,
			DATABASE,
			VAR_MODS,
			FIXED_MODS,
			ENZYME,
			MISSED_CLEAVAGES));
	public static final String CLEAVAGE_SEMI = "protein, cleavage semi";

	private Map<String, String> nativeParams = new HashMap<String, String>();
	private static final int ALLOW_NON_SPECIFIC_CLEAVAGES = 50;
	private static final String DALTONS = "Daltons";
	private Integer minTerminiCleavages;

	public XTandemMappings() {
	}

	@Override
	public Reader baseSettings() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/swift/params/base.tandem.xml.template", getClass());
	}

	@Override
	public void read(final Reader isr) {
		final Document bioml;
		try {
			bioml = XMLUtilities.loadDocument(isr);

			final Element doc = bioml.getDocumentElement();
			final NodeList notes = doc.getElementsByTagName("note");
			for (int i = 0; i < notes.getLength(); i++) {
				final Node it = notes.item(i);
				if (it.hasAttributes() && it.getAttributes().getNamedItem("type") != null &&
						"input".equals(it.getAttributes().getNamedItem("type").getTextContent())) {

					// TODO if the nextSibling has no type attribute, use it as the comment.
					final String id = it.getAttributes().getNamedItem("label").getTextContent();
					if (PARSED_PARAMS.contains(id) || IONS_PATTERN.matcher(id).matches()) {
						final String value = it.getTextContent();
						nativeParams.put(id, value);
					}
				}
			}
		} catch (Exception t) {
			throw new MprcException("Error reading X!Tandem parameter set", t);
		}
	}

	@Override
	public void write(final Reader oldParams, final Writer out) {
		final Document bioml;
		try {
			bioml = XMLUtilities.loadDocument(oldParams);

			final Element doc = bioml.getDocumentElement();
			final NodeList notes = doc.getElementsByTagName("note");
			for (int i = 0; i < notes.getLength(); i++) {
				final Node it = notes.item(i);
				if (it.hasAttributes() &&
						"input".equals(it.getAttributes().getNamedItem("type").getTextContent())) {

					// TODO if the nextSibling has no type attribute, use it as the comment.
					final String id = it.getAttributes().getNamedItem("label").getTextContent();
					if (nativeParams.keySet().contains(id)) {
						it.setTextContent(nativeParams.get(id));
					}
				}
			}
		} catch (Exception t) {
			throw new MprcException("Error reading X!Tandem parameter set", t);
		}

		// Prepare the output file
		final Result result = new StreamResult(out);

		// Write the DOM document to the file
		try {
			final Transformer xformer = TransformerFactory.newInstance().newTransformer();
			final DOMSource ds = new DOMSource(bioml.getDocumentElement());
			xformer.transform(ds, result);
		} catch (TransformerException e) {
			throw new MprcException("XML transformer error when writing tandem parameter set", e);
		} finally {
			FileUtilities.closeQuietly(out);
		}
	}

	@Override
	public String getNativeParam(final String name) {
		return nativeParams.get(name);
	}

	private void setNativeParam(final String name, final String value) {
		nativeParams.put(name, value);
	}

	@Override
	public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
		final double value = peptideTolerance.getValue();
		String unit = peptideTolerance.getUnit().getCode();
		if (peptideTolerance.getUnit().equals(MassUnit.Da)) {
			unit = DALTONS;
		}
		setNativeParam(PEP_TOL_PLUS, String.valueOf(value));
		setNativeParam(PEP_TOL_MINUS, String.valueOf(value));
		setNativeParam(PEP_TOL_UNIT, unit);
	}

	@Override
	public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
		if (!MassUnit.Da.equals(fragmentTolerance.getUnit())) {
			setNativeParam(FRAG_TOL_VALUE, "1");
			setNativeParam(FRAG_TOL_UNIT, DALTONS);
			context.reportWarning("Tandem does not support '" + fragmentTolerance.toString() + "' fragment tolerances; using 1 Da instead.", null);
			return;
		}
		setNativeParam(FRAG_TOL_VALUE, String.valueOf(fragmentTolerance.getValue()));
		setNativeParam(FRAG_TOL_UNIT, DALTONS);
	}

	@Override
	public void setVariableMods(final MappingContext context, final ModSet variableMods) {
		mapModsToNative(context, variableMods, VAR_MODS);
	}

	@Override
	public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
		mapModsToNative(context, fixedMods, FIXED_MODS);
	}

	private void mapModsToNative(final MappingContext context, final ModSet variableMods, final String modVariable) {
		final List<String> mods = new ArrayList<String>();

		for (final ModSpecificity ms : variableMods.getModifications()) {
			final String title = ms.toString();
			final double mass = ms.getModification().getMassMono();

			// "Protein [NC]-term"
			final boolean proteinNcTerm = ms.isProteinOnly() != null && ms.isProteinOnly();
			// we can't support specific amino acids at N or C terminus
			final boolean specificAminoAcidAtNorCterminus = !ms.isPositionAnywhere() && ms.isSiteSpecificAminoAcid();

			if ((ms.getTerm() != null) && (specificAminoAcidAtNorCterminus || proteinNcTerm)) {

				context.reportWarning("Tandem does not support " + (VAR_MODS.equals(modVariable) ? "variable" : "fixed") + " modification with position '" +
						ms.getTerm() + "' and site '" + ms.getSite() + "', dropping " + title, null);
			} else {
				String site = String.valueOf(ms.getSite());
				if (ms.isPositionNTerminus()) {
					site = "[";
				} else if (ms.isPositionCTerminus()) {
					site = "]";
				}

				mods.add(mass + "@" + site);
			}
		}
		setNativeParam(modVariable, makeCanonicalModsString(mods));
	}

	private static String makeCanonicalModsString(final List<String> mods) {
		final String[] modsArray = new String[mods.size()];
		mods.toArray(modsArray);
		Arrays.sort(modsArray);
		return Joiner.on(",").join(modsArray);
	}

	@Override
	public void setSequenceDatabase(final MappingContext context, final String shortDatabaseName) {
		setNativeParam(DATABASE, DATABASE_TAXON);
	}

	@Override
	public void setProtease(final MappingContext context, final Protease protease) {
		String cle = null;
		String rnminus1 = protease.getRnminus1();
		String rn = protease.getRn();

		if ((null != minTerminiCleavages && 0 == minTerminiCleavages) || (rn.isEmpty() && rnminus1.isEmpty())) {
			rnminus1 = "X";
			rn = "X";
			// handle the case where we set to 50 in allow non-specific cleavages
			setNativeParam(MISSED_CLEAVAGES, "50");
		} else {
			if (rnminus1.isEmpty()) {
				rnminus1 = "X";
			}
			if (rn.isEmpty()) {
				rn = "X";
			}
		}

		if (rnminus1.startsWith("!")) {
			cle = "{" + rnminus1.substring(1) + "}";
		} else {
			cle = "[" + rnminus1 + "]";
		}
		cle += "|";

		if (rn.startsWith("!")) {
			cle += "{" + rn.substring(1) + "}";
		} else {
			cle += "[" + rn + "]";
		}
		setNativeParam(ENZYME, cle);
	}

	@Override
	public void setMinTerminiCleavages(final MappingContext context, final Integer minTerminiCleavages) {
		this.minTerminiCleavages = minTerminiCleavages;
		if (minTerminiCleavages == 1) {
			setNativeParam(CLEAVAGE_SEMI, "yes");
		} else {
			setNativeParam(CLEAVAGE_SEMI, "no");
		}
	}

	@Override
	public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
		String value = null;

		try {
			if (missedCleavages != ALLOW_NON_SPECIFIC_CLEAVAGES) {
				//handle the case where we set to 50 in allow non-specific cleavages
				value = String.valueOf(missedCleavages);
			}
		} catch (NumberFormatException ignore) {
			// SWALLOWED
			context.reportWarning("Can't understand tandem missed cleavages: " + missedCleavages, null);
		}

		if (value != null) {
			setNativeParam(MISSED_CLEAVAGES, value);
		}
	}

	@Override
	public void setInstrument(final MappingContext context, final Instrument instrument) {
		final Map<String, IonSeries> hasSeries = new HashMap<String, IonSeries>();
		for (final IonSeries is : instrument.getSeries()) {
			hasSeries.put(is.getName(), is);
		}
		for (final String seriesName : SUPPORTED_IONS) {
			final String paramName = "scoring, " + seriesName + " ions";
			if (hasSeries.containsKey(seriesName)) {
				setNativeParam(paramName, "yes");
				hasSeries.remove(seriesName);
			} else {
				setNativeParam(paramName, "no");
			}
		}

		if (!hasSeries.isEmpty()) {
			context.reportWarning("Tandem doesn't support ion series " + Joiner.on(", ").join(hasSeries.keySet()), null);
		}
	}

	@Override
	public void checkValidity(MappingContext context) {
	}
}
