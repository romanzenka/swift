package edu.mayo.mprc.searchdb.builder;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.mayo.mprc.fastadb.PeptideSequence;
import edu.mayo.mprc.fastadb.ProteinSequence;
import edu.mayo.mprc.fastadb.ProteinSequenceTranslator;
import edu.mayo.mprc.searchdb.MassSpecDataExtractor;
import edu.mayo.mprc.searchdb.ScaffoldModificationFormat;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.searchdb.dao.IdentifiedPeptide;
import edu.mayo.mprc.searchdb.dao.LocalizedModBag;
import edu.mayo.mprc.searchdb.dao.LocalizedModification;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public class AnalysisBuilder implements Builder<Analysis> {
	/**
	 * To translate the Scaffold mods into actual Mod objects
	 */
	private ScaffoldModificationFormat format;

	/**
	 * To translate protein accession numbers into protein sequences.
	 */
	private ProteinSequenceTranslator translator;

	/**
	 * Give a name of a .RAW file, extracts various metadata.
	 */
	private MassSpecDataExtractor massSpecDataExtractor;

	/**
	 * Which search report (read = Scaffold file) does this Analysis link to?
	 */
	private ReportData reportData;

	/**
	 * Scaffold version as a string. Can be null if the version could not be determined.
	 */
	private String scaffoldVersion;

	/**
	 * Date and time of when the analysis was run, as recorded in the report. This information
	 * is somewhat duplicated (we know when the user submitted the search).
	 * It can be null if the date could not be determined.
	 */
	private DateTime analysisDate;

	/**
	 * A list of all biological samples defined within the Scaffold analysis report.
	 */
	private BiologicalSampleListBuilder biologicalSamples = new BiologicalSampleListBuilder(this);

	/**
	 * Cache of all protein sequences.
	 */
	private Map<String, ProteinSequence> proteinSequences = new HashMap<String, ProteinSequence>();

	/**
	 * Cache of all peptide sequences.
	 */
	private Map<String, PeptideSequence> peptideSequences = new HashMap<String, PeptideSequence>();

	/**
	 * Cache of all localized mods.
	 */
	private Map<LocalizedModification, LocalizedModification> localizedModifications = new HashMap<LocalizedModification, LocalizedModification>(100);

	/**
	 * Cache of all identified peptides.
	 */
	private Map<IdentifiedPeptide, IdentifiedPeptide> identifiedPeptides = new HashMap<IdentifiedPeptide, IdentifiedPeptide>(1000);

	public AnalysisBuilder(final ScaffoldModificationFormat format, final ProteinSequenceTranslator translator, final MassSpecDataExtractor massSpecDataExtractor) {
		this.format = format;
		this.translator = translator;
		this.massSpecDataExtractor = massSpecDataExtractor;
	}

	@Override
	public Analysis build() {
		return new Analysis(scaffoldVersion, analysisDate, biologicalSamples.build());
	}

	ProteinSequence getProteinSequence(final String accessionNumber, final String databaseSources) {
		final ProteinSequence proteinSequence = proteinSequences.get(accessionNumber);
		if (proteinSequence == null) {
			final ProteinSequence newProteinSequence = translator.getProteinSequence(accessionNumber, databaseSources);
			proteinSequences.put(accessionNumber, newProteinSequence);
			return newProteinSequence;
		}
		return proteinSequence;
	}

	/**
	 * @param peptideSequence Peptide sequence to cache and translate.
	 * @return The corresponding PeptideSequence object. The sequence is canonicalized to uppercase.
	 */
	PeptideSequence getPeptideSequence(final String peptideSequence) {
		final String upperCaseSequence = peptideSequence.toUpperCase(Locale.US);
		final PeptideSequence sequence = peptideSequences.get(upperCaseSequence);
		if (sequence == null) {
			final PeptideSequence newSequence = new PeptideSequence(upperCaseSequence);
			peptideSequences.put(upperCaseSequence, newSequence);
			return newSequence;
		}
		return sequence;
	}

	/**
	 * Get identified peptide.
	 *
	 * @param peptideSequence       The sequence of the peptide.
	 * @param fixedModifications    Fixed modifications parseable by {@link ScaffoldModificationFormat}.
	 * @param variableModifications Variable modifications parseable by {@link ScaffoldModificationFormat}.
	 * @return Unique identified peptide entry.
	 */
	IdentifiedPeptide getIdentifiedPeptide(
			final PeptideSequence peptideSequence,
			final String fixedModifications,
			final String variableModifications) {
		final Collection<LocalizedModification> mods = format.parseModifications(peptideSequence.getSequence(), fixedModifications, variableModifications);
		final LocalizedModBag mappedMods = new LocalizedModBag(Lists.transform(Lists.newArrayList(mods), mapLocalizedModification));

		final IdentifiedPeptide key = new IdentifiedPeptide(peptideSequence, mappedMods);
		final IdentifiedPeptide peptide = identifiedPeptides.get(key);
		if (peptide == null) {
			identifiedPeptides.put(key, key);
			return key;
		}
		return peptide;
	}

	/**
	 * Store each localized modification only once.
	 */
	private final Function<LocalizedModification, LocalizedModification> mapLocalizedModification = new Function<LocalizedModification, LocalizedModification>() {
		@Override
		public LocalizedModification apply(@Nullable final LocalizedModification from) {
			final LocalizedModification result = localizedModifications.get(from);
			if (result != null) {
				return result;
			}
			localizedModifications.put(from, from);
			return from;
		}
	};

	public ReportData getReportData() {
		return reportData;
	}

	public void setReportData(final ReportData reportData) {
		this.reportData = reportData;
	}

	public String getScaffoldVersion() {
		return scaffoldVersion;
	}

	public void setScaffoldVersion(final String scaffoldVersion) {
		this.scaffoldVersion = scaffoldVersion;
	}

	public DateTime getAnalysisDate() {
		return analysisDate;
	}

	public void setAnalysisDate(final DateTime analysisDate) {
		this.analysisDate = analysisDate;
	}

	public BiologicalSampleListBuilder getBiologicalSamples() {
		return biologicalSamples;
	}

	public ProteinSequenceTranslator getTranslator() {
		return translator;
	}

	public MassSpecDataExtractor getMassSpecDataExtractor() {
		return massSpecDataExtractor;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final AnalysisBuilder that = (AnalysisBuilder) o;

		if (analysisDate != null ? !analysisDate.equals(that.analysisDate) : that.analysisDate != null) {
			return false;
		}
		if (biologicalSamples != null ? !biologicalSamples.equals(that.biologicalSamples) : that.biologicalSamples != null) {
			return false;
		}
		if (reportData != null ? !reportData.equals(that.reportData) : that.reportData != null) {
			return false;
		}
		if (scaffoldVersion != null ? !scaffoldVersion.equals(that.scaffoldVersion) : that.scaffoldVersion != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = reportData != null ? reportData.hashCode() : 0;
		result = 31 * result + (scaffoldVersion != null ? scaffoldVersion.hashCode() : 0);
		result = 31 * result + (analysisDate != null ? analysisDate.hashCode() : 0);
		result = 31 * result + (biologicalSamples != null ? biologicalSamples.hashCode() : 0);
		return result;
	}
}
