package edu.mayo.mprc.omssa;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.xml.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OmssaMappings implements Mappings {
	private static final String DATABASE = "db";
	private static final String PEP_TOL = "peptol";
	private static final String FRAG_TOL = "msmstol";
	private static final String MISSED_CLEAVAGES = "missedcleave";

	private final Map<String, Node> nativeParams = new HashMap<String, Node>();
	private Document nativeParamsDocument;
	private UnimodOmssaConverter converter = new UnimodOmssaConverter(new ModsUtilities());
	private static final String ENZYME = "enzyme";
	private static final double AVG_PEPTIDE_MASS = 1000d;
	private static final double MILLION = 1000000d; // For part-per-million conversions

	public OmssaMappings() {
	}

	@Override
	public Reader baseSettings() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/swift/params/base.omssa.params.xml", getClass());
	}

	@Override
	public void read(final Reader isr) {
		final Document doc = XMLUtilities.loadDocument(isr);
		nativeParamsDocument = doc;
		final NodeList childNodes = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node node = childNodes.item(i);
			if (node.getNodeName().startsWith("MSSearchSettings_")) {
				final String id = node.getNodeName().replaceFirst("MSSearchSettings_", "");
				nativeParams.put(id, node);
			}
		}
	}

	@Override
	public void write(final Reader oldParams, final Writer out) {
		final Document bioml = nativeParamsDocument;
		// Write the DOM document to the file
		final StreamSource tSource = new StreamSource(new StringReader(XMLUtilities.INDENT_TRANSFORM_STRING));
		try {
			final Transformer xformer = TransformerFactory.newInstance().newTransformer(tSource);
			xformer.transform(new DOMSource(bioml.getDocumentElement()), new StreamResult(out));
		} catch (Exception t) {
			throw new MprcException("Cannot save omssa parameter file", t);
		}
	}

	@Override
	public String getNativeParam(final String name) {
		return nativeParams.get(name).getTextContent();
	}

	@Override
	public void setNativeParam(final String name, final String value) {
		nativeParams.get(name).setTextContent(value);
	}

	@Override
	public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
		if (!MassUnit.Da.equals(peptideTolerance.getUnit()) && !MassUnit.Ppm.equals(peptideTolerance.getUnit())) {
			//the user is trying to use ppm or an unsupported unit
			setNativeParam(PEP_TOL, "1");
			context.reportWarning("OMSSA does not support '" + peptideTolerance.getUnit() + "' tolerances; using 1 Dalton instead.");
			return;
		}

		final double value = convertToDalton(context, true, peptideTolerance);
		setNativeParam(PEP_TOL, String.valueOf(value));
	}

	private double convertToDalton(final MappingContext context, final boolean ppmAllowed, final Tolerance tolerance) {
		double value = tolerance.getValue();
		if (ppmAllowed && tolerance.getUnit().equals(MassUnit.Ppm)) {
			//convert ppm to Da assuming a average peptide mass of 1000 Da
			final double normMass = AVG_PEPTIDE_MASS;
			value = value * normMass / MILLION;
			context.reportWarning("Converted to " + value + " Da for OMSSA.");
		}
		return value;
	}

	@Override
	public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
		if (!fragmentTolerance.getUnit().equals(MassUnit.Da)) {
			//the user is trying to use ppm or an unsupported unit
			setNativeParam(FRAG_TOL, "1");
			context.reportWarning("OMSSA does not support '" + fragmentTolerance.getUnit() + "' tolerances; using 1 Dalton instead.");
		}
		//we can have a nice straight-forward conversion to Da from mmu
		final double value = convertToDalton(context, false, fragmentTolerance);
		setNativeParam(FRAG_TOL, String.valueOf(value));
	}

	@Override
	public void setVariableMods(final MappingContext context, final ModSet variableMods) {
		final Document doc = nativeParamsDocument;
		try {
			converter.convertUnimodToOmssa(/*fixed*/ false, variableMods.getModifications(), doc);
		} catch (Exception t) {
			throw new MprcException(t);
		}
	}

	@Override
	public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
		//this converter will insert necessary into the document
		try {
			converter.convertUnimodToOmssa(/*fixed*/ true, fixedMods.getModifications(), nativeParamsDocument);
		} catch (Exception t) {
			context.reportError(t.getMessage(), t);
		}
	}

	@Override
	public void setSequenceDatabase(final MappingContext context, final String shortDatabaseName) {
		setNativeParam(DATABASE, "${DB:" + shortDatabaseName + "}");
	}

	@Override
	public void setProtease(final MappingContext context, final Protease protease) {
		final String omssaId = EnzymeLookup.mapEnzymeAbstractToOmssa(protease.getName());

		if (omssaId == null) {
			context.reportWarning("OMSSA cannot support the enzyme " + protease.getName() + " so please disable OMSSA if you want this enzyme.");
			return; //don't change the enzyme
		}

		//remove previously selected enzymes.
		final Element enzymeElement = (Element) nativeParams.get(ENZYME);
		final NodeList previouslySelected = enzymeElement.getElementsByTagName("MSEnzymes");
		for (int i = previouslySelected.getLength() - 1; i >= 0; i--) {
			enzymeElement.removeChild(previouslySelected.item(i));
		}

		if (omssaId.contains("+")) {
			final String[] ids = omssaId.split("\\+");
			for (final String id : ids) {
				//all of the templates should have empty enzyme sections so just add them as children
				final Element newEnzyme = enzymeElement.getOwnerDocument().createElement("MSEnzymes");
				newEnzyme.setTextContent(id);
				enzymeElement.appendChild(newEnzyme);
			}
		} else {
			final Element newEnzyme = enzymeElement.getOwnerDocument().createElement("MSEnzymes");
			newEnzyme.setTextContent(omssaId);
			enzymeElement.appendChild(newEnzyme);
		}
	}

	@Override
	public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
		String value = null;
		try {
			value = String.valueOf(missedCleavages);
			setNativeParam(MISSED_CLEAVAGES, value);
		} catch (Exception t) {
			throw new MprcException("Can't understand OMSSA missed cleavages " + missedCleavages + " using " + value, t);
		}
	}

	@Override
	public void setInstrument(final MappingContext context, final Instrument instrument) {
		final Document doc = nativeParamsDocument;
		final List<String> ionSeriesIds = new ArrayList<String>();

		final List<String> unsupportedIons = new ArrayList<String>();
		for (final IonSeries ionSeries : instrument.getSeries()) {
			final String id = IonLookup.lookupEnum(ionSeries.getName());
			if (id == null) {
				unsupportedIons.add(ionSeries.getName());
			} else {
				ionSeriesIds.add(id);
			}
		}
		if (!unsupportedIons.isEmpty()) {
			context.reportWarning("OMSSA does not support ions: " + Joiner.on(", ").join(unsupportedIons));
		}
		final Element elemIons = (Element) nativeParams.get("ionstosearch");
		// Empty this element
		while (elemIons.hasChildNodes()) {
			elemIons.removeChild(elemIons.getFirstChild());
		}

		for (final String id : ionSeriesIds) {
			final Element newIon = doc.createElement("MSIonType");
			newIon.setTextContent(id);
			elemIons.appendChild(newIon);
		}
	}
}

