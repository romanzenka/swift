package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.MprcException;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class IdpQonvertSettings implements Serializable {
	private static final long serialVersionUID = 2585921464603786125L;

	public enum Option {
		CHARGE_STATE_HANDLING("ChargeStateHandling"),
		DECOY_PREFIX("DecoyPrefix"),
		EMBED_SPECTRUM_SCAN_TIMES("EmbedSpectrumScanTimes"),
		EMBED_SPECTRUM_SOURCES("EmbedSpectrumSources"),
		GAMMA("Gamma"),
		IGNORE_UNMAPPED_PEPTIDES("IgnoreUnmappedPeptides"),
		KERNEL("Kernel"),
		MASS_ERROR_HANDLING("MassErrorHandling"),
		MAX_FDR("MaxFDR"),
		MAX_IMPORT_FDR("MaxImportFDR"),
		MAX_RESULT_RANK("MaxResultRank"),
		MAX_TRAINING_RANK("MaxTrainingRank"),
		MIN_PARTITION_SIZE("MinPartitionSize"),
		MISSED_CLEAVAGES_HANDLING("MissedCleavagesHandling"),
		NU("Nu"),
		OUTPUT_SUFFIX("OutputSuffix"),
		OVERWRITE_EXISTING_FILES("OverwriteExistingFiles"),
		POLYNOMIAL_DEGREE("PolynomialDegree"),
		PREDICT_PROBABILITY("PredictProbability"),
		PROTEIN_DATABASE("ProteinDatabase"),
		QONVERTER_METHOD("QonverterMethod"),
		RERANK_MATCHES("RerankMatches"),
		SVMTYPE("SVMType"),
		SCORE_INFO("ScoreInfo"),
		SOURCE_SEARCH_PATH("SourceSearchPath"),
		TERMINAL_SPECIFICITY_HANDLING("TerminalSpecificityHandling"),
		TRUE_POSITIVE_THRESHOLD("TruePositiveThreshold"),
		WRITE_QONVERSION_DETAILS("WriteQonversionDetails");


		private final String key;

		Option(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public static Option forKey(final String key) {
			for (final Option option : Option.values()) {
				if (option.getKey().equalsIgnoreCase(key)) {
					return option;
				}
			}
			throw new MprcException("Unknown idpQonvert option " + key);
		}
	}

	private String chargeStateHandling = "Partition";
	private String decoyPrefix = "rev_";
	private boolean embedSpectrumScanTimes = false;
	private boolean embedSpectrumSources = false;
	private double gamma = 5.0;
	private boolean ignoreUnmappedPeptides = false;
	private String kernel = "Linear";
	private String massErrorHandling = "Ignore";
	private double maxFDR = 0.05;
	private double maxImportFDR = 0.25;
	private int maxResultRank = 3;
	private int maxTrainingRank = 1;
	private int minPartitionSize = 10;
	private String missedCleavagesHandling = "Ignore";
	private double nu = 0.5;
	private String outputSuffix = "";
	private boolean overwriteExistingFiles = false;
	private int polynomialDegree = 3;
	private boolean predictProbability = true;
	private String proteinDatabase = "";
	private String qonverterMethod = "MonteCarlo";
	private boolean rerankMatches = false;
	private String svmType = "CSVC";
	private String scoreInfo = "1 off myrimatch:mvh; 1 off xcorr; 1 off sequest:xcorr; 1 off sequest:deltacn; 1 off mascot:score; -1 off x!tandem:expect; 1 off x!tandem:hyperscore; -1 off ms-gf:specevalue; -1 off evalue";
	private String sourceSearchPath = ".;..";
	private String terminalSpecificityHandling = "Partition";
	private double truePositiveThreshold = 0.01;
	private boolean writeQonversionDetails = false;

	public IdpQonvertSettings() {
	}

	private static String toggle(final boolean value) {
		return value ? "1" : "0";
	}

	private static DecimalFormat FORMAT = new DecimalFormat("0.########");

	private static String dbl(final double value) {
		return FORMAT.format(value);
	}

	/**
	 * @return The list of settings as a big string to be written into a config file.
	 *         The settings should have the same effect as if specified on the command line.
	 */
	public String toConfigFile() {
		final StringBuilder builder = new StringBuilder(2000);

		for (final Option option : Option.values()) {
			builder.append(option.getKey());
			builder.append("=\"");
			builder.append(getValue(option));
			builder.append("\"\n");
		}

		return builder.toString();
	}

	private static void ra(final List<String> result, final Option key, final String value) {
		result.add('-' + key.getKey());
		result.add(value);
	}

	public List<String> toCommandLine() {
		List<String> result = new ArrayList<String>(Option.values().length * 2);

		for (final Option option : Option.values()) {
			ra(result, option, getValue(option));
		}
		return result;
	}

	public String getValue(final Option option) {
		switch (option) {

			case CHARGE_STATE_HANDLING:
				return getChargeStateHandling();
			case DECOY_PREFIX:
				return getDecoyPrefix();
			case EMBED_SPECTRUM_SCAN_TIMES:
				return toggle(isEmbedSpectrumScanTimes());
			case EMBED_SPECTRUM_SOURCES:
				return toggle(isEmbedSpectrumSources());
			case GAMMA:
				return dbl(getGamma());
			case IGNORE_UNMAPPED_PEPTIDES:
				return toggle(isIgnoreUnmappedPeptides());
			case KERNEL:
				return getKernel();
			case MASS_ERROR_HANDLING:
				return getMassErrorHandling();
			case MAX_FDR:
				return dbl(getMaxFDR());
			case MAX_IMPORT_FDR:
				return dbl(getMaxImportFDR());
			case MAX_RESULT_RANK:
				return Integer.toString(getMaxResultRank());
			case MAX_TRAINING_RANK:
				return Integer.toString(getMaxTrainingRank());
			case MIN_PARTITION_SIZE:
				return Integer.toString(getMinPartitionSize());
			case MISSED_CLEAVAGES_HANDLING:
				return getMissedCleavagesHandling();
			case NU:
				return dbl(getNu());
			case OUTPUT_SUFFIX:
				return getOutputSuffix();
			case OVERWRITE_EXISTING_FILES:
				return toggle(isOverwriteExistingFiles());
			case POLYNOMIAL_DEGREE:
				return Integer.toString(getPolynomialDegree());
			case PREDICT_PROBABILITY:
				return toggle(isPredictProbability());
			case PROTEIN_DATABASE:
				return getProteinDatabase();
			case QONVERTER_METHOD:
				return getQonverterMethod();
			case RERANK_MATCHES:
				return toggle(isRerankMatches());
			case SVMTYPE:
				return getSvmType();
			case SCORE_INFO:
				return getScoreInfo();
			case SOURCE_SEARCH_PATH:
				return getSourceSearchPath();
			case TERMINAL_SPECIFICITY_HANDLING:
				return getTerminalSpecificityHandling();
			case TRUE_POSITIVE_THRESHOLD:
				return dbl(getTruePositiveThreshold());
			case WRITE_QONVERSION_DETAILS:
				return toggle(isWriteQonversionDetails());
			default:
				throw new MprcException("Unsupported IdpQonvert option: " + option.getKey());
		}
	}

	public String getChargeStateHandling() {
		return chargeStateHandling;
	}

	public void setChargeStateHandling(String chargeStateHandling) {
		this.chargeStateHandling = chargeStateHandling;
	}

	public String getDecoyPrefix() {
		return decoyPrefix;
	}

	public void setDecoyPrefix(String decoyPrefix) {
		this.decoyPrefix = decoyPrefix;
	}

	public boolean isEmbedSpectrumScanTimes() {
		return embedSpectrumScanTimes;
	}

	public void setEmbedSpectrumScanTimes(boolean embedSpectrumScanTimes) {
		this.embedSpectrumScanTimes = embedSpectrumScanTimes;
	}

	public boolean isEmbedSpectrumSources() {
		return embedSpectrumSources;
	}

	public void setEmbedSpectrumSources(boolean embedSpectrumSources) {
		this.embedSpectrumSources = embedSpectrumSources;
	}

	public double getGamma() {
		return gamma;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public boolean isIgnoreUnmappedPeptides() {
		return ignoreUnmappedPeptides;
	}

	public void setIgnoreUnmappedPeptides(boolean ignoreUnmappedPeptides) {
		this.ignoreUnmappedPeptides = ignoreUnmappedPeptides;
	}

	public String getKernel() {
		return kernel;
	}

	public void setKernel(String kernel) {
		this.kernel = kernel;
	}

	public String getMassErrorHandling() {
		return massErrorHandling;
	}

	public void setMassErrorHandling(String massErrorHandling) {
		this.massErrorHandling = massErrorHandling;
	}

	public double getMaxFDR() {
		return maxFDR;
	}

	public void setMaxFDR(double maxFDR) {
		this.maxFDR = maxFDR;
	}

	public double getMaxImportFDR() {
		return maxImportFDR;
	}

	public void setMaxImportFDR(double maxImportFDR) {
		this.maxImportFDR = maxImportFDR;
	}

	public int getMaxResultRank() {
		return maxResultRank;
	}

	public void setMaxResultRank(int maxResultRank) {
		this.maxResultRank = maxResultRank;
	}

	public int getMaxTrainingRank() {
		return maxTrainingRank;
	}

	public void setMaxTrainingRank(int maxTrainingRank) {
		this.maxTrainingRank = maxTrainingRank;
	}

	public int getMinPartitionSize() {
		return minPartitionSize;
	}

	public void setMinPartitionSize(int minPartitionSize) {
		this.minPartitionSize = minPartitionSize;
	}

	public String getMissedCleavagesHandling() {
		return missedCleavagesHandling;
	}

	public void setMissedCleavagesHandling(String missedCleavagesHandling) {
		this.missedCleavagesHandling = missedCleavagesHandling;
	}

	public double getNu() {
		return nu;
	}

	public void setNu(double nu) {
		this.nu = nu;
	}

	public String getOutputSuffix() {
		return outputSuffix;
	}

	public void setOutputSuffix(String outputSuffix) {
		this.outputSuffix = outputSuffix;
	}

	public boolean isOverwriteExistingFiles() {
		return overwriteExistingFiles;
	}

	public void setOverwriteExistingFiles(boolean overwriteExistingFiles) {
		this.overwriteExistingFiles = overwriteExistingFiles;
	}

	public int getPolynomialDegree() {
		return polynomialDegree;
	}

	public void setPolynomialDegree(int polynomialDegree) {
		this.polynomialDegree = polynomialDegree;
	}

	public boolean isPredictProbability() {
		return predictProbability;
	}

	public void setPredictProbability(boolean predictProbability) {
		this.predictProbability = predictProbability;
	}

	public String getProteinDatabase() {
		return proteinDatabase;
	}

	public void setProteinDatabase(String proteinDatabase) {
		this.proteinDatabase = proteinDatabase;
	}

	public String getQonverterMethod() {
		return qonverterMethod;
	}

	public void setQonverterMethod(String qonverterMethod) {
		this.qonverterMethod = qonverterMethod;
	}

	public boolean isRerankMatches() {
		return rerankMatches;
	}

	public void setRerankMatches(boolean rerankMatches) {
		this.rerankMatches = rerankMatches;
	}

	public String getSvmType() {
		return svmType;
	}

	public void setSvmType(String svmType) {
		this.svmType = svmType;
	}

	public String getScoreInfo() {
		return scoreInfo;
	}

	public void setScoreInfo(String scoreInfo) {
		this.scoreInfo = scoreInfo;
	}

	public String getSourceSearchPath() {
		return sourceSearchPath;
	}

	public void setSourceSearchPath(String sourceSearchPath) {
		this.sourceSearchPath = sourceSearchPath;
	}

	public String getTerminalSpecificityHandling() {
		return terminalSpecificityHandling;
	}

	public void setTerminalSpecificityHandling(String terminalSpecificityHandling) {
		this.terminalSpecificityHandling = terminalSpecificityHandling;
	}

	public double getTruePositiveThreshold() {
		return truePositiveThreshold;
	}

	public void setTruePositiveThreshold(double truePositiveThreshold) {
		this.truePositiveThreshold = truePositiveThreshold;
	}

	public boolean isWriteQonversionDetails() {
		return writeQonversionDetails;
	}

	public void setWriteQonversionDetails(boolean writeQonversionDetails) {
		this.writeQonversionDetails = writeQonversionDetails;
	}
}
