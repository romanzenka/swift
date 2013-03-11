package edu.mayo.mprc.unimod;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.ComparisonChain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A set of Modifications.
 */
public class IndexedModSet implements Set<Mod> {
	/**
	 * How many characters does one mod take in the report on average.
	 */
	private static final int REPORT_ENTRY_SIZE = 100;
	private static final Pattern CLEAN_COMMENTS = Pattern.compile("\\s+");

	private String name;
	/**
	 * all modification in this set
	 */
	protected Set<Mod> modifications = new TreeSet<Mod>();
	/**
	 * an index to allow fast look between a name (title or alternative names) and the associated Modification object.
	 */
	private Map<String, Mod> titleIndex = new HashMap<String, Mod>();
	/**
	 * A sorted map of the Delta's sorted by Monoisotopic mass.  This will allow a fast searching of mods in a given mass range.
	 */
	private NavigableMap<Double, List<Mod>> deltaMonoMassIndex = new TreeMap<Double, List<Mod>>();

	public IndexedModSet() {

	}

	/**
	 * takes a modification and adds it to the indexes
	 *
	 * @param m the Modification you want to have added to indices.
	 */
	private void index(final Mod m) {
		titleIndex.put(m.getTitle(), m);
		if (m.getAltNames() != null) {
			for (final String altName : m.getAltNames()) {
				titleIndex.put(altName, m);
			}
		}

		List<Mod> massList = this.deltaMonoMassIndex.get(m.getMassMono());
		if (massList == null) {
			massList = new LinkedList<Mod>();
		}
		massList.add(m);
		deltaMonoMassIndex.put(m.getMassMono(), massList);
	}

	/**
	 * Return mod for given title.
	 *
	 * @param title the title that you want ta Modification of
	 * @return the Modification that matches the given title
	 */
	public Mod getByTitle(final String title) {
		return titleIndex.get(title);
	}

	/**
	 * Return a mod by record ID.
	 *
	 * @param recordId ID of the mod.
	 * @return Mod of a given id, or null if none found.
	 */
	public Mod getByRecordId(final int recordId) {
		for (final Mod mod : modifications) {
			if (recordId == mod.getRecordID()) {
				return mod;
			}
		}
		return null;
	}

	public static final class MascotNameParts {
		private String title;
		private String protein;
		private String term;
		private String acids;
		/**
		 * Parses Mascot specificity, that is in form [1=Title] ([2=Protein]? [3=N/C-term]? [4=list of amino acids]?)
		 */
		public static final Pattern MASCOT_SPECIFICITY = Pattern.compile("\\s*(.+?)\\s*\\(\\s*((?:Protein)?)\\s*((?:[NC]-term)?)\\s*([^)]*)?\\)");

		public MascotNameParts(final String title, final String protein, final String term, final String acids) {
			this.title = title;
			this.protein = protein;
			this.term = term;
			this.acids = acids;
		}

		public String getTitle() {
			return title;
		}

		public String getProtein() {
			return protein;
		}

		public String getTerm() {
			return term;
		}

		public String getAcids() {
			return acids;
		}

		public static MascotNameParts parseMascotName(final String mascotName) {
			final Matcher matcher = MASCOT_SPECIFICITY.matcher(mascotName);
			if (matcher.matches()) {
				return new MascotNameParts(
						matcher.group(1),
						matcher.group(2),
						matcher.group(3),
						matcher.group(4));
			}
			return null;
		}
	}

	/**
	 * Get a modification specificities.
	 * The input is mascot name for a modification, examples:
	 * <ul>
	 * <li>Title (ST)</li>
	 * <li>Title (Protein N-term A)</li>
	 * <li>Title (Protein C-term)</li>
	 * <li>Title (C-term T)</li>
	 * <li>Title (C-term)</li>
	 * </ul>
	 */
	public List<ModSpecificity> getSpecificitiesByMascotName(final String mascotName) {
		final MascotNameParts nameParts = MascotNameParts.parseMascotName(mascotName);
		if (nameParts != null) {
			final Mod modification = getByTitle(nameParts.getTitle());
			if (modification == null) {
				throw new MprcException("Cannot find modification named " + nameParts.getTitle());
			}
			final boolean proteinEnd = "Protein".equalsIgnoreCase(nameParts.getProtein());
			final Terminus terminus;
			if ("N-term".equalsIgnoreCase(nameParts.getTerm())) {
				terminus = Terminus.Nterm;
			} else if ("C-term".equalsIgnoreCase(nameParts.getTerm())) {
				terminus = Terminus.Cterm;
			} else {
				terminus = Terminus.Anywhere;
			}

			final List<ModSpecificity> modSpecificities = new ArrayList<ModSpecificity>();
			for (final ModSpecificity modSpecificity : modification.getModSpecificities()) {
				if (modSpecificity.matches(nameParts.getAcids(), terminus, proteinEnd)) {
					modSpecificities.add(modSpecificity);
				}
			}
			return modSpecificities;
		} else {
			throw new MprcException("Cannot understand modification: " + mascotName);
		}
	}

	/**
	 * Takes a name of a Modification or a modification's alias and finds the other aliases.
	 *
	 * @param title the name you want to find aliases for
	 * @return all of the aliases for the given name or null if name is not found, if there is not alternative names then empty list is returned.
	 */
	public Set<String> getAlternativeNames(final String title) {
		final Set<String> retSet = new HashSet<String>();
		final Mod mod = getByTitle(title);
		if (mod == null) {
			return null;
		}
		retSet.add(mod.getTitle());
		retSet.addAll(mod.getAltNames());
		retSet.remove(title);
		return retSet;
	}

	/**
	 * Finds the ModificationSpecifities that could occur for specified parameters.
	 * <p/>
	 * For instance, if we want Q on N-terminus, then all mods that modify Q anywhere match, all mods that modify anything on N-term will match,
	 * as well as mods that specifically modify Q at the N-terminus.
	 * <p/>
	 * This means that different matches have different quality. We attempt to sort them by the closeness of the match as follows:
	 * <p/>
	 * 1) first comes anything that matches exactly what you asked for, ordered by distance from
	 * <p/>
	 * <p/>
	 * Requirement set to null is not considered.
	 * For a set of all modifications, set all parameters to null.
	 *
	 * @param mass        the expected mass
	 * @param maxDelta    the maximum delta allowed
	 * @param site        the specificity site. If null, we do not care. The site can be more specific than the actual mod and still match. E.g. we ask for Q, but the mod allows "anywhere".
	 * @param terminus    the specificity terminus. The terminus can be more specific than the mod. For instance, if we are asking for N-term and the mod allows "anywhere", it will match.
	 * @param proteinOnly if terminus is set, specify if it has to be protein terminus
	 * @param hidden      whether the modification is hidden or not
	 * @return a list of admissible modifications, ordered by the quality of the match
	 */
	public List<ModSpecificity> findMatchingModSpecificities(final Double mass, final Double maxDelta, final Character site, final Terminus terminus, final Boolean proteinOnly, final Boolean hidden) {
		Collection<List<Mod>> inRange = null;
		if (mass != null && maxDelta != null) {
			inRange = deltaMonoMassIndex.subMap(mass - maxDelta, mass + maxDelta).values();
		} else {
			inRange = deltaMonoMassIndex.values();
		}

		final Collection<ModSpecificity> modsInMassRange = collectModSpecificities(inRange);

		final ArrayList<ModSpecificityMatch> matchingModSpecificities = new ArrayList<ModSpecificityMatch>();
		for (final ModSpecificity sp : modsInMassRange) {
			final ModSpecificityMatch match = ModSpecificityMatch.match(sp, mass, maxDelta, site, terminus, proteinOnly, hidden);
			if (match != null) {
				matchingModSpecificities.add(match);
			}
		}

		return Lists.transform(
				Ordering.natural().sortedCopy(matchingModSpecificities),
				ModSpecificityMatch.GET_MOD_SPECIFICITY);
	}

	/**
	 * Captures how well did a given mod specificity match.
	 */
	private static final class ModSpecificityMatch implements Comparable<ModSpecificityMatch> {
		private final ModSpecificity matchingModSpecificity;
		private final int siteMatch;
		private final int terminusMatch;
		private final int proteinOnlyMatch;
		private final int hiddenMatch;
		private final double massMatch;

		private ModSpecificityMatch(final ModSpecificity matchingModSpecificity, final int siteMatch, final int terminusMatch, final int proteinOnlyMatch, final int hiddenMatch, final double massMatch) {
			this.matchingModSpecificity = matchingModSpecificity;
			this.siteMatch = siteMatch;
			this.terminusMatch = terminusMatch;
			this.proteinOnlyMatch = proteinOnlyMatch;
			this.hiddenMatch = hiddenMatch;
			this.massMatch = massMatch;
		}

		public static final Function<ModSpecificityMatch, ModSpecificity> GET_MOD_SPECIFICITY = new Function<ModSpecificityMatch, ModSpecificity>() {
			@Override
			public ModSpecificity apply(final ModSpecificityMatch from) {
				return from == null ? null : from.getMatchingModSpecificity();
			}
		};

		public static ModSpecificityMatch match(final ModSpecificity sp, final Double mass, final Double maxDelta, final Character site, final Terminus terminus, final Boolean proteinOnly, final Boolean hidden) {
			final int siteMatch = siteMatches(site, sp);
			if (siteMatch == 0) {
				return null;
			}
			final int terminusMatch = terminusMatches(terminus, sp);
			if (terminusMatch == 0) {
				return null;
			}
			final int proteinOnlyMatch = proteinOnly == null ? 1 : proteinOnly.equals(sp.isProteinOnly()) ? 10 : 0;
			final int hiddenMatch = hidden == null ? (sp.getHidden() == true ? 1 : 2) : hidden.equals(sp.getHidden()) ? 10 : 0;
			double delta = 0.0;
			if (mass != null) {
				delta = Math.abs(sp.getModification().getMassMono() - mass);
				if (maxDelta != null && delta > maxDelta) {
					return null;
				}
			}
			return new ModSpecificityMatch(sp, siteMatch, terminusMatch, proteinOnlyMatch, hiddenMatch, delta);
		}

		public ModSpecificity getMatchingModSpecificity() {
			return matchingModSpecificity;
		}

		/**
		 * Either we have exact match (we require terminus, mod has the terminus), or
		 * the specificity does not require particular terminus.
		 *
		 * @param terminus Expected to be non-null.
		 * @param sp       Specificity to match.
		 * @return 0 - no match, 10 - perfect match
		 */
		private static int terminusMatches(final Terminus terminus, final ModSpecificity sp) {
			if (terminus == null) {
				return 1;
			}
			if (sp.getTerm() == terminus) {
				return 10;
			}
			if (sp.getTerm() == Terminus.Anywhere) {
				return 5;
			}
			return 0;
		}

		/**
		 * Either we have an exact match, or the mod specificity allows any site.
		 *
		 * @param expectedSite Site we expect to see.
		 * @param sp           Specificity to match the site against.
		 * @return 0 - no match, 10 - perfect match
		 */
		private static int siteMatches(final Character expectedSite, final ModSpecificity sp) {
			if (expectedSite == null) {
				return 1;
			}

			final char site = Character.toUpperCase(expectedSite);

			if (sp.getSite().equals(expectedSite)) {
				return 10;
			}
			// We do not have an exact match. Let's go down by priority.

			// - Handle asp/asn ambiguity
			if ('B' == site) {
				if (sp.getSite().equals('D') || sp.getSite().equals('N')) {
					return 7;
				}
			}

			// - Handle glu/gln ambiguity
			if ('Z' == site) {
				if (sp.getSite().equals('E') || sp.getSite().equals('Q')) {
					return 7;
				}
			}

			// - the site denotes any amino acid.
			if ('X' == site) {
				return 6;
			}

			// - the mod is not specific to a particular amino acid
			if (!sp.isSiteSpecificAminoAcid()) {
				return 5;
			}
			return 0;
		}

		/**
		 * Site match is the most important, then terminus, augmented by protein-only flag.
		 * Hidden flag is least important, lastly the mass delta is taken into account.
		 * <p/>
		 * The larger, the better, so the comparisions are flipped, except the last one.
		 * <p/>
		 * If everything is the same, the modification record id is used.
		 */
		@Override
		public int compareTo(final ModSpecificityMatch o) {
			return ComparisonChain.start()
					.compare(o.siteMatch, siteMatch)
					.compare(o.terminusMatch, terminusMatch)
					.compare(o.proteinOnlyMatch, proteinOnlyMatch)
					.compare(o.hiddenMatch, hiddenMatch)
					.compare(o.massMatch, massMatch)
					.compare(matchingModSpecificity.getModification().getRecordID(), o.getMatchingModSpecificity().getModification().getRecordID())
					.result();
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			return compareTo((ModSpecificityMatch) o) == 0;
		}

		@Override
		public int hashCode() {
			int result;
			final long temp;
			result = siteMatch;
			result = 31 * result + terminusMatch;
			result = 31 * result + proteinOnlyMatch;
			result = 31 * result + hiddenMatch;
			temp = massMatch != +0.0d ? Double.doubleToLongBits(massMatch) : 0L;
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
	}

	private static Set<ModSpecificity> collectModSpecificities(final Collection<List<Mod>> inRange) {
		final Set<ModSpecificity> modsInMassRange = new HashSet<ModSpecificity>();
		for (final List<Mod> l : inRange) {
			for (final Mod m : l) {
				for (final ModSpecificity ms : m.getModSpecificities()) {
					modsInMassRange.add(ms);
				}
			}
		}
		return modsInMassRange;
	}

	/**
	 * This method works just as {@link #findMatchingModSpecificities} except if multiple results are found
	 * then a little more intelligence is used to give just one.  Only use this if it really doesn't matter as long as the given parameters match.
	 * <p/>
	 * Currently this only takes into account the 'hidden' property so that ones that are not hidden are chosen over those that are
	 */
	public ModSpecificity findSingleMatchingModificationSet(final Double mass, final Double maxDelta, final Character site, final Terminus terminus, final Boolean proteinOnly, final Boolean hidden) {
		final List<ModSpecificity> allMatches = new ArrayList<ModSpecificity>(findMatchingModSpecificities(mass, maxDelta, site, terminus, proteinOnly, hidden));

		if (allMatches.isEmpty()) {
			return null;
		} else if (allMatches.size() == 1) {
			return allMatches.get(0);
		} else {
			Collections.sort(allMatches, new Comparator<ModSpecificity>() {
				//try to determine which specificity is more favorable

				//favor non-hidden over hidden specificities.

				public int compare(final ModSpecificity o1, final ModSpecificity o2) {
					//want hidden ones to be higher in the list
					return Boolean.valueOf(o1.getHidden()).compareTo(o2.getHidden());
				}
			});

			return allMatches.get(0);
		}
	}

	/**
	 * gets all of the official titles of all of modifications
	 *
	 * @return a list of all titles
	 */
	public Set<String> getAllTitles() {
		final Set<String> titleSet = new HashSet<String>();
		for (final Mod m : modifications) {
			titleSet.add(m.getTitle());
		}
		return titleSet;
	}

	public Set<Mod> asSet() {
		return Collections.unmodifiableSet(modifications);
	}

	public Set<ModSpecificity> getAllSpecificities(final boolean includeHidden) {
		final Set<ModSpecificity> modspecset = new TreeSet<ModSpecificity>();
		for (final Mod modification : modifications) {
			for (final ModSpecificity modspec : modification.getModSpecificities()) {
				if (includeHidden || !modspec.getHidden()) {
					modspecset.add(modspec);
				}
			}
		}
		return modspecset;
	}

	/**
	 * @return A map from full_name to the monoisotopic mass.
	 */
	public Map<String, Double> getFullNameToMonoisotopicMassMap() {
		final Map<String, Double> map = new HashMap<String, Double>(modifications.size());
		for (final Mod mod : modifications) {
			map.put(mod.getFullName(), mod.getMassMono());
			map.put(mod.getTitle(), mod.getMassMono());
		}
		return map;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public int size() {
		return modifications.size();
	}

	@Override
	public boolean isEmpty() {
		return modifications.isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return modifications.contains((Mod) o);
	}

	@Override
	public Iterator<Mod> iterator() {
		return modifications.iterator();
	}

	@Override
	public Object[] toArray() {
		return modifications.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return modifications.toArray(a);
	}

	@Override
	public boolean add(final Mod toAdd) {
		final boolean added = this.modifications.add(toAdd);
		if (added) {
			this.index(toAdd);
		}
		return added;
	}

	@Override
	public boolean remove(final Object o) {
		throw new MprcException("A modification set does not support removing its elements");
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return modifications.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends Mod> c) {
		final boolean added = modifications.addAll(c);
		if (added) {
			for (final Mod mod : c) {
				this.index(mod);
			}
		}
		return added;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		throw new MprcException("A modification set does not support retainAll");
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		throw new MprcException("A modification set does not support removeAll");
	}

	@Override
	public void clear() {
		throw new MprcException("A modification set does not support clearing");
	}

	public int compareTo(final Object t) {
		if (!(t instanceof IndexedModSet)) {
			return 1;
		}
		final IndexedModSet tt = (IndexedModSet) t;
		ComparisonChain chain = ComparisonChain.start().nullsFirst();
		chain = chain
				.compare(this.name, tt.name)
				.compare(this.modifications.size(), tt.modifications.size());
		if (chain.result() == 0) {
			for (Iterator<Mod> i = this.modifications.iterator(), j = tt.modifications.iterator(); i.hasNext(); ) {
				final Mod left = i.next();
				final Mod right = j.next();
				chain = chain.compare(left, right);
				if (chain.result() != 0) {
					break;
				}
			}
		}
		return chain.result();
	}

	/**
	 * Dump entire modification set into a large tsv report.
	 *
	 * @return HTML table describing in detail all the peculiarities of modifications defined in this set.
	 */
	public String report() {
		final StringBuilder result = new StringBuilder(modifications.size() * REPORT_ENTRY_SIZE);
		result.append("<table>\n<tr><th>Record Id</th><th>Title</th><th>Full Name</th><th>Mono Mass</th><th>Average Mass</th><th>Composition</th><th>Alt Names</th><th>" +
				"Specificity Site</th><th>Specificity Terminus</th><th>Specificity Protein Only</th><th>Specificity Group</th><th>Hidden</th><th>Comments</th></tr>\n");
		for (final Mod mod : modifications) {
			final TreeSet<ModSpecificity> orderedModSpecificities = new TreeSet<ModSpecificity>(mod.getModSpecificities());
			for (final ModSpecificity specificity : orderedModSpecificities) {
				result.append("<tr><td>").append(mod.getRecordID())
						.append("</td><td>").append(mod.getTitle())
						.append("</td><td>").append(mod.getFullName())
						.append("</td><td>").append(mod.getMassMono())
						.append("</td><td>").append(mod.getMassAverage())
						.append("</td><td>").append(mod.getComposition())
						.append("</td><td>").append(cleanWhitespace(Joiner.on(", ").join(new TreeSet<String>(mod.getAltNames()))))
						.append("</td><td>").append(specificity.getSite())
						.append("</td><td>").append(specificity.getTerm())
						.append("</td><td>").append(specificity.isPositionProteinSpecific())
						.append("</td><td>").append(specificity.getSpecificityGroup())
						.append("</td><td>").append(specificity.getHidden())
						.append("</td><td>").append(cleanWhitespace(specificity.getComments()))
						.append("</td></tr>\n");
			}
		}
		result.append("</table>");
		return result.toString();
	}

	static String cleanWhitespace(final String text) {
		if (text == null) {
			return "";
		}
		return CLEAN_COMMENTS.matcher(text).replaceAll(" ");
	}

	public boolean equals(final Object obj) {
		if (!(obj instanceof IndexedModSet)) {
			return false;
		}
		final IndexedModSet tt = (IndexedModSet) obj;
		return Objects.equal(this.name, tt.name) &&
				Objects.equal(this.modifications, tt.modifications);
	}

	public int hashCode() {
		return (name != null ? name.hashCode() : 0) + modifications.hashCode();
	}

}
