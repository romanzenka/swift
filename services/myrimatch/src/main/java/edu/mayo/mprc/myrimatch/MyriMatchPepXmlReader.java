package edu.mayo.mprc.myrimatch;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.KeyedTsvReader;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class MyriMatchPepXmlReader extends DefaultHandler implements KeyedTsvReader {

	/**
	 * Header.
	 */
	private static final String[] DEFAULT_HEADER = new String[]{
			"MyriMatch Peptide", "MyriMatch Protein",
			"MyriMatch Total Proteins", "MyriMatch Num Matched Ions", "MyriMatch Total Num Ions",
			"MyriMatch mvh", "MyriMatch mz Fidelity", "MyriMatch xcorr"
	};
	private static final String EMPTY_LINE;
	public static final int INITIAL_NUM_SPECTRA = 1000;

	static {
		// One less tab - we produce tabs only in between values
		EMPTY_LINE = StringUtilities.repeat('\t', DEFAULT_HEADER.length - 1);
	}

	private Map<Integer, String> lineInformation;

	private int scan;
	private String peptide;
	private String protein;
	private String totalProteins;
	private String matchedIons;
	private String totalIons;
	private String mvh;
	private String mzFidelity;
	private String xcorr;
	private boolean rank1;

	public MyriMatchPepXmlReader() {
	}

	/**
	 * Load the pepXML file from a stream. The stream is closed upon completion.
	 *
	 * @param pepXML Stream to load pepXML from.
	 */
	public void load(final InputStream pepXML) {
		try {
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			lineInformation = new HashMap<Integer, String>(INITIAL_NUM_SPECTRA);
			parser.parse(pepXML, this);
		} catch (Exception e) {
			throw new MprcException("Could not parse myrimatch pepXML file", e);
		} finally {
			FileUtilities.closeQuietly(pepXML);
		}
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		if (qName.equals("spectrum_query")) {
			scan = Integer.valueOf(attributes.getValue("start_scan"));
		} else if (qName.equals("search_hit")) {
			if ("1".equals(attributes.getValue("hit_rank"))) {
				rank1 = true;
				peptide = attributes.getValue("peptide");
				protein = attributes.getValue("protein");
				totalProteins = attributes.getValue("num_tot_proteins");
				matchedIons = attributes.getValue("num_matched_ions");
				totalIons = attributes.getValue("tot_num_ions");
			} else {
				rank1 = false;
			}
		}
		if (rank1 && qName.equals("search_score")) {
			if ("mvh".equals(attributes.getValue("name"))) {
				mvh = attributes.getValue("value");
			} else if ("mzFidelity".equals(attributes.getValue("name"))) {
				mzFidelity = attributes.getValue("value");
			} else if ("xcorr".equals(attributes.getValue("name"))) {
				xcorr = attributes.getValue("value");
			}
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		if (qName.equals("spectrum_query")) {
			lineInformation.put(scan,
					peptide + "\t" + protein + "\t" + totalProteins + "\t"
							+ matchedIons + "\t" + totalIons + "\t"
							+ mvh + "\t" + mzFidelity + "\t" + xcorr);
		}
	}

	@Override
	public String getHeaderLine() {
		return Joiner.on("\t").join(DEFAULT_HEADER);
	}

	@Override
	public String getEmptyLine() {
		return EMPTY_LINE;
	}

	/**
	 * @param key The index of the original .mgf spectrum (numbered from 0)
	 * @return Information from myrimatch for the particular line.
	 */
	@Override
	public String getLineForKey(final String key) {
		final int spectrum = Integer.valueOf(key);
		final String result = lineInformation.get(spectrum);
		if (result == null) {
			return getEmptyLine();
		}
		return result;
	}
}
