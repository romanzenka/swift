package edu.mayo.mprc.swift.ui.client.widgets.validation;

import edu.mayo.mprc.common.client.StringUtilities;
import edu.mayo.mprc.swift.ui.client.CompareClientModSpecificity;
import edu.mayo.mprc.swift.ui.client.rpc.ClientModSpecificity;

import java.util.*;

/**
 * Supports search of modifications
 * <p>
 * The ways to search are
 * <ul>
 * <li> record ID
 * <li> any words in @ClientModSpecificity
 * <li> delta mass (field @ClientModSpecificity.monoisotopic). If user enters -1 search to +/-1 Dalton, if -1.1 search to 0.1 Dalton
 * </p>
 * This class is a singleton
 */
public final class ModificationSearch {
	private List<ClientModSpecificity> allowedValues;

	private Map<String, ClientModSpecificity> records;
	private Map<String, List<WordMap>> words;

	private ModificationSearch(final List<ClientModSpecificity> allowedValues) {

		this.allowedValues = allowedValues;
		initialize();
	}

	private ModificationSearch() {

	}

	public void setAllowedValues(final List<ClientModSpecificity> allowedValues) {
		this.allowedValues = allowedValues;
		initialize();
	}

	private void initialize() {
		if (allowedValues == null) {
			return;
		}
		// build record index
		records = new HashMap<String, ClientModSpecificity>(allowedValues.size() * 3 / 2);
		// load the records index
		for (final ClientModSpecificity allowedValue : allowedValues) {
			addRecordToMap(allowedValue);
		}
		// create the wordmap
		words = createWordHash();
	}

	private void addRecordToMap(final ClientModSpecificity specificity) {
		records.put("" + specificity.getRecordID() + specificity.getSite(), specificity);
	}

	public static ModificationSearch createInstance(final List<ClientModSpecificity> allowedValues) {

		return new ModificationSearch(allowedValues);

	}

	public static ModificationSearch createInstance() {
		return new ModificationSearch();
	}

	/**
	 * given an search specification do the lookup
	 *
	 * @param spec - could be word, or deltamass or record id
	 * @return array of ClientModSpecificity
	 */
	public List<ClientModSpecificity> search(final String spec) {
		if (spec == null || spec.isEmpty() || spec.trim().equals("")) {
			return allowedValues;
		}
		List<ClientModSpecificity> result;

		result = searchByMass(spec);

		if (result == null || result.isEmpty()) {
			result = searchByWord(spec);
			// TODO record id
			if (result.isEmpty()) {
				// search by record id
				final ClientModSpecificity singleResult = searchByRecordId(spec);
				if (singleResult != null) {
					result = new ArrayList<ClientModSpecificity>(1);
					result.add(singleResult);
					return result;
				}
				// otherwise
				result = allowedValues;
			}
		}
		Collections.sort(result, new CompareClientModSpecificity());
		return result;
	}

	public static native boolean match(String toMatch) /*-{
        var numPatt = /\d+|(\d+\.\d*)|(\.\d+)/;
        var result = toMatch.match(numPatt);
        if (result && result.length > 0) {
            return true;
        }
        return false;
    }-*/;

	/**
	 * search by monoisotopic delta mass
	 *
	 * @param massWindow - specifies mass window
	 *                   <p>
	 *                   <ul>
	 *                   <li> '<mass>-1' then search -1 and +1 Dalton
	 *                   <li> '<mass>-1.1' search for 0.9 Dalton to 1.1 Dalton window
	 *                   </ul>
	 *                   </p>
	 * @return the array of ClientModSpecificity's found. If the mass window does not parse, null.
	 */
	public List<ClientModSpecificity> searchByMass(final String massWindow) {
		// regular expressions cannot be used here as GWT1.4 does not support them
		final MassPrecision mp = new MassPrecision();
		final boolean found = parseMassWindow(massWindow, mp);
		if (!found) {
			return null;
		}
		if (mp.getPrecision() == 0.0) {
			mp.setPrecision(1.0);
		}

		return getByMass(mp.getMass(), mp.getPrecision());
	}

	/**
	 * has <mass><-><precision>
	 */
	private static final String PATTERN_RECORD = "^[#](\\d+)$";

	/**
	 * Find the record having the record Id
	 *
	 * @param spec - the expression
	 * @return records with that id
	 */
	public ClientModSpecificity searchByRecordId(final String spec) {
		final boolean matches = spec.matches(PATTERN_RECORD);

		if (!matches) {
			return null;
		}
		final int recordId = Integer.parseInt((spec.trim()).substring(1));
		return records.get(String.valueOf(recordId));
	}

	/**
	 * find the items containing the word in the name, altnames or comments section
	 *
	 * @param word
	 * @return List of the items found by name. Never null, can be an empty list though.
	 */
	public List<ClientModSpecificity> searchByWord(final String word) {
		// an ArrayList  of ArrayList of WordMap
		final List<List<WordMap>> wordMaps = findWords(word);
		// to find the size need to get size of each contained array, there could be duplicates so create a HashMap
		// based on record id
		final HashSet<ClientModSpecificity> found = new HashSet<ClientModSpecificity>();
		for (final List<WordMap> someWords : wordMaps) {
			for (final WordMap w : someWords) {
				final ClientModSpecificity specificity = records.get("" + w.getRecordId() + w.getSite());
				if (!found.contains(specificity)) {
					found.add(specificity);
				}
			}
		}
		final List<ClientModSpecificity> result = new ArrayList<ClientModSpecificity>(found.size());
		result.addAll(found);
		return result;
	}

	/**
	 * find the records containing the word
	 *
	 * @param subSequence
	 * @return
	 */
	private List<List<WordMap>> findWords(final String subSequence) {
		final List<List<WordMap>> wordmaps = new ArrayList<List<WordMap>>();
		if (words == null) {
			initialize();
		}
		final String subSequenceLower = StringUtilities.toLowerCase(subSequence);
		for (final String word : words.keySet()) {
			final String word_lower = StringUtilities.toLowerCase(word);
			if (word_lower.indexOf(subSequenceLower) != -1) {
				wordmaps.add(words.get(word));
			}
		}
		return wordmaps;
	}


	private List<ClientModSpecificity> getByMass(final double mass, final double precision) {
		final List<ClientModSpecificity> items = new ArrayList<ClientModSpecificity>();
		final double lower = mass - precision;
		final double upper = mass + precision;
		findByMass(lower, upper, items);
		findByMass(lower * -1.0, upper * -1.0, items);
		return items;
	}

	private void findByMass(final double lower, final double upper, final List<ClientModSpecificity> items) {
		for (final ClientModSpecificity allowedValue : allowedValues) {
			final double other = allowedValue.getMonoisotopic();
			if (other <= upper && other >= lower) {
				items.add(allowedValue);
			}
		}
	}

	/**
	 * stores information about a word that was found
	 */
	private static final class WordMap {
		private int recordId;
		private String site;

		public int getRecordId() {
			return recordId;
		}

		public void setRecordId(final int recordId) {
			this.recordId = recordId;
		}

		public String getSite() {
			return site;
		}

		public void setSite(final String site) {
			this.site = site;
		}
	}

	/**
	 * for the word search will build a hash to allow exact match
	 * with each word will be kept a record id and the field name that provided it
	 */
	private Map<String, List<WordMap>> createWordHash() {
		final Map<java.lang.String, java.util.List<WordMap>> words = new HashMap<String, List<WordMap>>();
		// traverse the name, altname and comments fields finding the words
		for (final ClientModSpecificity allowedValue : allowedValues) {
			String[] w = allowedValue.getName().split(" ");
			if (w != null) {
				updateHash(w, "name", words, allowedValue);
			}
			final List<String> altNames = allowedValue.getAltNames();
			if (altNames != null) {
				for (final String altName : altNames) {
					w = altName.split(" ");
					if (w != null) {
						updateHash(w, "altNames", words, allowedValue);
					}
				}
			}
			if (allowedValue.getComments() != null) {
				w = allowedValue.getComments().split(" ");
				if (w != null) {
					updateHash(w, "comments", words, allowedValue);
				}
			}
		}
		return words;
	}

	private void updateHash(final String[] w, final String fieldname, final Map<String, List<WordMap>> words, final ClientModSpecificity c) {
		if (w != null) {
			for (String word : w) {
				// trim the word of any characters such a \n\r\t
				word = word.trim();
				if (word == null || "".equals(word)) {
					continue;
				}
				List<WordMap> r = words.get(word);
				// if r does not exist create it
				if (r == null) {
					r = new ArrayList<WordMap>();
					words.put(word, r);
				}
				final WordMap m = new WordMap();
				m.setRecordId(c.getRecordID());
				m.setSite(String.valueOf(c.getSite()));
				r.add(m);
			}
		}
	}

	private static final class MassPrecision {
		private double mass;
		private double precision;

		public double getMass() {
			return mass;
		}

		public void setMass(final double mass) {
			this.mass = mass;
		}

		public double getPrecision() {
			return precision;
		}

		public void setPrecision(final double precision) {
			this.precision = precision;
		}
	}

	/**
	 * not properly done in the GWT1.4 emulation library
	 * It implements some of the functionality of {@link java.util.StringTokenizer}
	 */
	private static final class StringTokenizer {
		private String input;
		private String delimiters;
		private boolean includeDelimiter;
		private int index;
		private List<String> tokens;

		StringTokenizer(final String input, final String delimiters, final boolean includeDelimiter) {
			this.input = input;
			this.delimiters = delimiters;
			this.includeDelimiter = includeDelimiter;
			tokens = new ArrayList<String>();
			parse();
		}

		public int countTokens() {
			return tokens.size();
		}

		public boolean hasMoreTokens() {
			return index < tokens.size();
		}

		public String nextToken() {
			return tokens.get(index++);
		}

		private void parse() {
			final char[] chars = input.toCharArray();
			final char[] delims = delimiters.toCharArray();
			int start = 0;
			for (int i = 0; i < chars.length; i++) {
				for (final char delim : delims) {
					if (chars[i] == delim) {
						if (i > start) {
							tokens.add(new String(chars, start, i - start));
						}
						if (includeDelimiter) {
							tokens.add("" + delim);
						}
						start = i + 1;
						break;
					}
				}
			}
			if (start < chars.length) {
				tokens.add(new String(chars, start, chars.length - start));
			}
		}
	}

	/**
	 * This function is here because GWT1.4 does not have regular expressions
	 * a valid mass search specification can have any of the following
	 * <negative> <number>
	 * <number>
	 * <number><negative><number>
	 * <negative><number><negative><number>
	 * these represent a number followed by an optional precision
	 * {<negative>}<number>{<negative><precision>}
	 * so there will be 1, 2, 3 or 4 tokens
	 *
	 * @return true if matched pattern for a mass specification and modifies contents of {link #holder}
	 *         otherwise returns false
	 */
	private boolean parseMassWindow(final String input, final MassPrecision holder) {
		final String from = input.trim();
		final StringTokenizer t;
		t = new StringTokenizer(from, "-", true/* include - as token*/);
		final int numTokens = t.countTokens();
		final List<String> tokens = new ArrayList<String>();
		while (t.hasMoreTokens()) {
			tokens.add(t.nextToken());
		}

		// based on the number of tokens the expected structure is known
		if (numTokens == 1) {
			// first and only token must be a number
			return getMass(holder, tokens.get(0), 1);
		}
		if (numTokens == 2) {
			// have a negative number
			return getNegativeMass(holder, tokens.get(0), tokens.get(1));
		}
		if (numTokens == 3) {
			// have a positive number followed by a precision
			final boolean have = getMass(holder, tokens.get(0), 1);
			if (!have) {
				return false;
			}
			return getPrecision(holder, tokens.get(1), tokens.get(2));
		}
		if (numTokens == 4) {
			// have a negative number followed by a precision
			final boolean have = getNegativeMass(holder, tokens.get(0), tokens.get(1));
			if (!have) {
				return false;
			}
			return getPrecision(holder, tokens.get(2), tokens.get(3));
		}
		return false;
	}

	/**
	 * Parses the precision. It expects the pattern <negative><number>
	 *
	 * @param holder - precision stored here
	 * @param token1 - expected <negative> token
	 * @param token2 - expected <number> token
	 * @return false if does not match pattern other true and modifies {link #holder}
	 */
	private boolean getPrecision(final MassPrecision holder, final String token1, final String token2) {
		// first token must be negative
		if (!("-").equals(token1)) {
			return false;
		}
		try {
			holder.setPrecision(Double.valueOf(token2));
		} catch (Exception ignore) {
			return false;
		}
		return true;
	}

	/**
	 * parses negative mass. It expects <negative><number>
	 *
	 * @param holder - store mass here
	 * @param token1 - token expected to be negative
	 * @param token2 - token expected to be number
	 * @return true if matched the expected pattern, and adds the mass to holder
	 *         false if did not match the expected pattern of <negative><number>
	 */
	private boolean getNegativeMass(final MassPrecision holder, final String token1, final String token2) {
		// first token must be negative
		if (!(token1).equals("-")) {
			return false;
		}
		return getMass(holder, token2, -1);
	}

	/**
	 * Expects number. Parses the mass and places in {link #holder}
	 *
	 * @param holder
	 * @param token
	 * @param multiplier
	 * @return
	 */
	private boolean getMass(final MassPrecision holder, final String token, final int multiplier) {
		try {
			holder.setMass(Double.valueOf(token) * multiplier);
		} catch (Exception ignore) {
			return false;
		}
		return true;
	}


}
