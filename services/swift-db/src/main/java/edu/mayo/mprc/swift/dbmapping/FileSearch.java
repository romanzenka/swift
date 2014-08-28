package edu.mayo.mprc.swift.dbmapping;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.EqualityCriteria;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.io.File;

/**
 * Information about how to search a single input file.
 */
public class FileSearch extends PersistableBase implements EqualityCriteria {
	/**
	 * File to be searched.
	 */
	private File inputFile;
	/**
	 * Biological sample name (Scaffold column).
	 */
	private String biologicalSample;
	/**
	 * Category name (several samples can belong to one category). When null, the category is "none".
	 */
	private String categoryName;
	/**
	 * Experiment name (Scaffold file).
	 */
	private String experiment;

	private SwiftSearchDefinition swiftSearchDefinition;

	private SearchEngineParameters searchParameters;

	public FileSearch() {
	}

	public FileSearch(final File inputFile, final String biologicalSample, final String categoryName, final String experiment, final SearchEngineParameters searchParameters) {
		this.inputFile = inputFile;
		this.biologicalSample = biologicalSample;
		this.categoryName = categoryName;
		this.experiment = experiment;
		this.searchParameters = searchParameters;
	}

	public File getInputFile() {
		return inputFile;
	}

	public void setInputFile(final File inputFile) {
		this.inputFile = inputFile;
	}

	public String getBiologicalSample() {
		return biologicalSample;
	}

	void setBiologicalSample(final String biologicalSample) {
		this.biologicalSample = biologicalSample;
	}

	public String getCategoryName() {
		return categoryName;
	}

	void setCategoryName(final String categoryName) {
		this.categoryName = categoryName;
	}

	public String getExperiment() {
		return experiment;
	}

	public void setExperiment(final String experiment) {
		this.experiment = experiment;
	}

	public SwiftSearchDefinition getSwiftSearchDefinition() {
		return swiftSearchDefinition;
	}

	public void setSwiftSearchDefinition(SwiftSearchDefinition swiftSearchDefinition) {
		this.swiftSearchDefinition = swiftSearchDefinition;
	}

	public SearchEngineParameters getSearchParameters() {
		return searchParameters;
	}

	public SearchEngineParameters getSearchParametersWithDefault(final SearchEngineParameters defaultParameters) {
		return searchParameters == null ? defaultParameters : searchParameters;
	}

	public void setSearchParameters(SearchEngineParameters searchParameters) {
		this.searchParameters = searchParameters;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof FileSearch)) {
			return false;
		}

		final FileSearch that = (FileSearch) o;

		if (getBiologicalSample() != null ? !getBiologicalSample().equals(that.getBiologicalSample()) : that.getBiologicalSample() != null) {
			return false;
		}
		if (getCategoryName() != null ? !getCategoryName().equals(that.getCategoryName()) : that.getCategoryName() != null) {
			return false;
		}
		if (getExperiment() != null ? !getExperiment().equals(that.getExperiment()) : that.getExperiment() != null) {
			return false;
		}
		if (getInputFile() != null ? !getInputFile().equals(that.getInputFile()) : that.getInputFile() != null) {
			return false;
		}
		if (getSwiftSearchDefinition() != null ? !getSwiftSearchDefinition().shallowEquals(that.getSwiftSearchDefinition()) : that.getSwiftSearchDefinition() != null) {
			return false;
		}
		if (getSearchParameters() != null ? !getSearchParameters().equals(that.getSearchParameters()) : that.getSearchParameters() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getInputFile() != null ? getInputFile().hashCode() : 0;
		result = 31 * result + (getBiologicalSample() != null ? getBiologicalSample().hashCode() : 0);
		result = 31 * result + (getCategoryName() != null ? getCategoryName().hashCode() : 0);
		result = 31 * result + (getExperiment() != null ? getExperiment().hashCode() : 0);
		result = 31 * result + (getSearchParameters() != null ? getSearchParameters().hashCode() : 0);
		return result;
	}

	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("inputFile", getInputFile()))
				.add(DaoBase.nullSafeEq("biologicalSample", getBiologicalSample()))
				.add(DaoBase.nullSafeEq("categoryName", getCategoryName()))
				.add(DaoBase.nullSafeEq("experiment", getExperiment()))
				.add(DaoBase.associationEq("swiftSearchDefinition", getSwiftSearchDefinition()))
				.add(DaoBase.associationEq("searchParameters", getSearchParameters()));
	}

}
