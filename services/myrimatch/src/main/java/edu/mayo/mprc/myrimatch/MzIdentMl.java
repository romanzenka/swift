package edu.mayo.mprc.myrimatch;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.List;

/**
 * MzIdentML processing
 *
 * @author Roman Zenka
 */
public final class MzIdentMl {
	private MzIdentMl() {
	}

	public static String retitle(final Object input, final Object titles) {
		if (!(titles instanceof List)) {
			ExceptionUtilities.throwCastException(titles, List.class);
			return "";
		}
		final List<String> spectraTitles = (List<String>) titles;
		if (!(input instanceof String)) {
			ExceptionUtilities.throwCastException(input, String.class);
			return "";
		}
		final String spectrumId = (String) input;
		if (spectrumId.startsWith("index=")) {
			final String spectrumIdString = spectrumId.substring("index=".length());
			final int spectrumNumber;
			try {
				spectrumNumber = Integer.parseInt(spectrumIdString);
			} catch (NumberFormatException e) {
				throw new MprcException("Spectrum ID [" + spectrumId + "] does not denote a numerical spectrum index", e);
			}
			if (spectrumNumber >= 0 && spectrumNumber < spectraTitles.size()) {
				return spectraTitles.get(spectrumNumber);
			} else {
				throw new MprcException("Invalid spectrum index " + spectrumNumber + ". Must be within 0-" + (spectraTitles.size() - 1));
			}
		} else {
			throw new MprcException("Unexepected spectrum ID format: " + spectrumId);
		}
	}

	public static void replace(final File input, final List<String> spectraTitles, final File output) {
		try {
			final TransformerFactory transformerFactory = TransformerFactory.newInstance();
			final Source xslt = new StreamSource(ResourceUtilities.getReader("classpath:edu/mayo/mprc/myrimatch/replaceSpectraTitles.xsl", MzIdentMl.class));
			final Transformer transformer = transformerFactory.newTransformer(xslt);
			transformer.setParameter("titles", spectraTitles);

			final Source inputText = new StreamSource(input);
			final StreamResult outputText = new StreamResult(output);
			transformer.transform(inputText, outputText);
		} catch (Exception e) {
			throw new MprcException("Could not transform mzIdentML file " + input.getAbsolutePath() + " into " + output.getAbsolutePath(), e);
		}
	}
}
