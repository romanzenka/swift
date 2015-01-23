package edu.mayo.mprc.quameterdb.dao;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.searchdb.dao.SearchResult;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public final class QuameterResult extends PersistableBase {
	/**
	 * Files that have _Pre or _Post after the standard prefix (copath, patient, date) are ignored.
	 */
	private static final Pattern PRE_POST = Pattern.compile("^.{14}.*_(Pre|Post).*$");

	/**
	 * A search result points to {@link edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample} and
	 * {@link edu.mayo.mprc.searchdb.dao.ProteinGroupList} which makes it a great candidate
	 * to get to all metadata about one particular {@code .RAW} file.
	 */
	private SearchResult searchResult;

	/**
	 * We also keep information about the settings for the particular file. This is our link for Swift's
	 * search settings.
	 */
	private FileSearch fileSearch;
	/**
	 * This is not serialized to the database, but the DAO fills this item for convenience.
	 * It is the category for the file search
	 */
	private transient String category;
	private int transaction;

	/**
	 * Comment for the special "hidden" metric is hijacked to be used as a comment for hiding
	 * of the entire result.
	 * <p/>
	 * DAO fills this from the database. Setting this value is done through saving a special
	 * {@link edu.mayo.mprc.quameterdb.dao.QuameterAnnotation}.
	 */
	private transient String hiddenReason;

	public enum QuameterColumn {
		c_1a,
		c_1b,
		c_2a,
		c_2b,
		c_3a,
		c_3b,
		c_4a,
		c_4b,
		c_4c,

		/* Dynamic sampling */
		ds_1a,
		ds_1b,
		ds_2a,
		ds_2b,
		ds_3a,
		ds_3b,

		/* Ion source */
		is_1a,
		is_1b,
		is_2,
		is_3a,
		is_3b,
		is_3c,

		/* MS1 signal */
		ms1_1,
		ms1_2a,
		ms1_2b,
		ms1_3a,
		ms1_3b,
		ms1_5a,
		ms1_5b,
		ms1_5c,
		ms1_5d,

		/* MS2 signal */
		ms2_1,
		ms2_2,
		ms2_3,
		ms2_4a,
		ms2_4b,
		ms2_4c,
		ms2_4d,

		/* Peptides */
		p_1,
		p_2a,
		p_2b,
		p_2c,
		p_3,
	}

	/* Chromatography */
	private double c_1a;
	private double c_1b;
	private double c_2a;
	private double c_2b;
	private double c_3a;
	private double c_3b;
	private double c_4a;
	private double c_4b;
	private double c_4c;

	/* Dynamic sampling */
	private double ds_1a;
	private double ds_1b;
	private double ds_2a;
	private double ds_2b;
	private double ds_3a;
	private double ds_3b;

	/* Ion source */
	private double is_1a;
	private double is_1b;
	private double is_2;
	private double is_3a;
	private double is_3b;
	private double is_3c;

	/* MS1 signal */
	private double ms1_1;
	private double ms1_2a;
	private double ms1_2b;
	private double ms1_3a;
	private double ms1_3b;
	private double ms1_5a;
	private double ms1_5b;
	private double ms1_5c;
	private double ms1_5d;

	/* MS2 signal */
	private double ms2_1;
	private double ms2_2;
	private double ms2_3;
	private double ms2_4a;
	private double ms2_4b;
	private double ms2_4c;
	private double ms2_4d;

	/* Peptides */
	private double p_1;
	private double p_2a;
	private double p_2b;
	private double p_2c;
	private double p_3;

	/* User can mark the particular value as hidden */
	private boolean hidden;

	/**
	 * How many spectra total did we identify for a particular protein group
	 */
	private Map<QuameterProteinGroup, Integer/* Unique spectra */> identifiedSpectra;

	public QuameterResult() {
	}

	public QuameterResult(final SearchResult searchResult,
	                      final FileSearch fileSearch,
	                      final Map<String, Double> values,
	                      final Map<QuameterProteinGroup, Integer> identifiedSpectra) {
		this.searchResult = searchResult;
		this.fileSearch = fileSearch;
		this.identifiedSpectra = identifiedSpectra;
		setValues(values);
	}

	public SearchResult getSearchResult() {
		return searchResult;
	}

	public void setSearchResult(SearchResult searchResult) {
		this.searchResult = searchResult;
	}

	public FileSearch getFileSearch() {
		return fileSearch;
	}

	public void setFileSearch(final FileSearch fileSearch) {
		this.fileSearch = fileSearch;
	}

	private QuameterColumn getColumnByName(final String name) {
		Preconditions.checkArgument(name != null);
		final String canonicalName = name.toLowerCase(Locale.US).replace('-', '_');
		try {
			return QuameterColumn.valueOf(canonicalName);
		} catch (final IllegalArgumentException e) {
			throw new MprcException("Could not find QuaMeter column with name [" + name + "]. Available names are " +
					Joiner.on(", ").join(QuameterColumn.values()), e);
		}
	}

	public static String getColumnName(final QuameterColumn column) {
		return column.name().toUpperCase(Locale.US).replace('_', '-');
	}

	/**
	 * Set value.
	 *
	 * @param name  Quameter column name as it appears in the .qual.txt file.
	 * @param value Value to be set.
	 */
	public void setValue(final String name, final double value) {
		final QuameterColumn column = getColumnByName(name);
		setValue(column, value);
	}

	private void setValue(final QuameterColumn column, final double value) {
		switch (column) {
			case c_1a:
				setC_1a(value);
				break;
			case c_1b:
				setC_1b(value);
				break;
			case c_2a:
				setC_2a(value);
				break;
			case c_2b:
				setC_2b(value);
				break;
			case c_3a:
				setC_3a(value);
				break;
			case c_3b:
				setC_3b(value);
				break;
			case c_4a:
				setC_4a(value);
				break;
			case c_4b:
				setC_4b(value);
				break;
			case c_4c:
				setC_4c(value);
				break;
			case ds_1a:
				setDs_1a(value);
				break;
			case ds_1b:
				setDs_1b(value);
				break;
			case ds_2a:
				setDs_2a(value);
				break;
			case ds_2b:
				setDs_2b(value);
				break;
			case ds_3a:
				setDs_3a(value);
				break;
			case ds_3b:
				setDs_3b(value);
				break;
			case is_1a:
				setIs_1a(value);
				break;
			case is_1b:
				setIs_1b(value);
				break;
			case is_2:
				setIs_2(value);
				break;
			case is_3a:
				setIs_3a(value);
				break;
			case is_3b:
				setIs_3b(value);
				break;
			case is_3c:
				setIs_3c(value);
				break;
			case ms1_1:
				setMs1_1(value);
				break;
			case ms1_2a:
				setMs1_2a(value);
				break;
			case ms1_2b:
				setMs1_2b(value);
				break;
			case ms1_3a:
				setMs1_3a(value);
				break;
			case ms1_3b:
				setMs1_3b(value);
				break;
			case ms1_5a:
				setMs1_5a(value);
				break;
			case ms1_5b:
				setMs1_5b(value);
				break;
			case ms1_5c:
				setMs1_5c(value);
				break;
			case ms1_5d:
				setMs1_5d(value);
				break;
			case ms2_1:
				setMs2_1(value);
				break;
			case ms2_2:
				setMs2_2(value);
				break;
			case ms2_3:
				setMs2_3(value);
				break;
			case ms2_4a:
				setMs2_4a(value);
				break;
			case ms2_4b:
				setMs2_4b(value);
				break;
			case ms2_4c:
				setMs2_4c(value);
				break;
			case ms2_4d:
				setMs2_4d(value);
				break;
			case p_1:
				setP_1(value);
				break;
			case p_2a:
				setP_2a(value);
				break;
			case p_2b:
				setP_2b(value);
				break;
			case p_2c:
				setP_2c(value);
				break;
			case p_3:
				setP_3(value);
				break;
			default:
				throw new MprcException("Programmer error: missing QuaMeter column case statement for " + column.toString());
		}
	}


	public double getValue(final String name) {
		final QuameterColumn column = getColumnByName(name);
		return getValue(column);
	}

	public double getValue(final QuameterColumn column) {
		switch (column) {
			case c_1a:
				return getC_1a();

			case c_1b:
				return getC_1b();

			case c_2a:
				return getC_2a();

			case c_2b:
				return getC_2b();

			case c_3a:
				return getC_3a();

			case c_3b:
				return getC_3b();

			case c_4a:
				return getC_4a();

			case c_4b:
				return getC_4b();

			case c_4c:
				return getC_4c();

			case ds_1a:
				return getDs_1a();

			case ds_1b:
				return getDs_1b();

			case ds_2a:
				return getDs_2a();

			case ds_2b:
				return getDs_2b();

			case ds_3a:
				return getDs_3a();

			case ds_3b:
				return getDs_3b();

			case is_1a:
				return getIs_1a();

			case is_1b:
				return getIs_1b();

			case is_2:
				return getIs_2();

			case is_3a:
				return getIs_3a();

			case is_3b:
				return getIs_3b();

			case is_3c:
				return getIs_3c();

			case ms1_1:
				return getMs1_1();

			case ms1_2a:
				return getMs1_2a();

			case ms1_2b:
				return getMs1_2b();

			case ms1_3a:
				return getMs1_3a();

			case ms1_3b:
				return getMs1_3b();

			case ms1_5a:
				return getMs1_5a();

			case ms1_5b:
				return getMs1_5b();

			case ms1_5c:
				return getMs1_5c();

			case ms1_5d:
				return getMs1_5d();

			case ms2_1:
				return getMs2_1();

			case ms2_2:
				return getMs2_2();

			case ms2_3:
				return getMs2_3();

			case ms2_4a:
				return getMs2_4a();

			case ms2_4b:
				return getMs2_4b();

			case ms2_4c:
				return getMs2_4c();

			case ms2_4d:
				return getMs2_4d();

			case p_1:
				return getP_1();

			case p_2a:
				return getP_2a();

			case p_2b:
				return getP_2b();

			case p_2c:
				return getP_2c();

			case p_3:
				return getP_3();
			default:
				throw new MprcException("Programmer error: missing QuaMeter column case statement for " + column.toString());
		}
	}

	public Map<String, Double> getValues() {
		final ImmutableMap.Builder<String, Double> builder = new ImmutableMap.Builder<String, Double>();
		for (final QuameterColumn column : QuameterColumn.values()) {
			builder.put(getColumnName(column), getValue(column));
		}
		return builder.build();
	}

	public void setValues(final Map<String, Double> values) {
		if (values == null) {
			return;
		}
		for (final Map.Entry<String, Double> entry : values.entrySet()) {
			setValue(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Determine if given QuameterResult should be listed.
	 *
	 * @return True if the result should be displayed in the UI.
	 */
	public boolean resultMatches() {
		// We must not be a Pre or Postblank file
		if (PRE_POST.matcher(getFileSearch().getInputFile().getName()).find()) {
			return false;
		}
		return true;
	}

	public double getC_1a() {
		return c_1a;
	}

	public void setC_1a(final double c_1a) {
		this.c_1a = c_1a;
	}

	public double getC_1b() {
		return c_1b;
	}

	public void setC_1b(final double c_1b) {
		this.c_1b = c_1b;
	}

	public double getC_2a() {
		return c_2a;
	}

	public void setC_2a(final double c_2a) {
		this.c_2a = c_2a;
	}

	public double getC_2b() {
		return c_2b;
	}

	public void setC_2b(final double c_2b) {
		this.c_2b = c_2b;
	}

	public double getC_3a() {
		return c_3a;
	}

	public void setC_3a(final double c_3a) {
		this.c_3a = c_3a;
	}

	public double getC_3b() {
		return c_3b;
	}

	public void setC_3b(final double c_3b) {
		this.c_3b = c_3b;
	}

	public double getC_4a() {
		return c_4a;
	}

	public void setC_4a(final double c_4a) {
		this.c_4a = c_4a;
	}

	public double getC_4b() {
		return c_4b;
	}

	public void setC_4b(final double c_4b) {
		this.c_4b = c_4b;
	}

	public double getC_4c() {
		return c_4c;
	}

	public void setC_4c(final double c_4c) {
		this.c_4c = c_4c;
	}

	public double getDs_1a() {
		return ds_1a;
	}

	public void setDs_1a(final double ds_1a) {
		this.ds_1a = ds_1a;
	}

	public double getDs_1b() {
		return ds_1b;
	}

	public void setDs_1b(final double ds_1b) {
		this.ds_1b = ds_1b;
	}

	public double getDs_2a() {
		return ds_2a;
	}

	public void setDs_2a(final double ds_2a) {
		this.ds_2a = ds_2a;
	}

	public double getDs_2b() {
		return ds_2b;
	}

	public void setDs_2b(final double ds_2b) {
		this.ds_2b = ds_2b;
	}

	public double getDs_3a() {
		return ds_3a;
	}

	public void setDs_3a(final double ds_3a) {
		this.ds_3a = ds_3a;
	}

	public double getDs_3b() {
		return ds_3b;
	}

	public void setDs_3b(final double ds_3b) {
		this.ds_3b = ds_3b;
	}

	public double getIs_1a() {
		return is_1a;
	}

	public void setIs_1a(final double is_1a) {
		this.is_1a = is_1a;
	}

	public double getIs_1b() {
		return is_1b;
	}

	public void setIs_1b(final double is_1b) {
		this.is_1b = is_1b;
	}

	public double getIs_2() {
		return is_2;
	}

	public void setIs_2(final double is_2) {
		this.is_2 = is_2;
	}

	public double getIs_3a() {
		return is_3a;
	}

	public void setIs_3a(final double is_3a) {
		this.is_3a = is_3a;
	}

	public double getIs_3b() {
		return is_3b;
	}

	public void setIs_3b(final double is_3b) {
		this.is_3b = is_3b;
	}

	public double getIs_3c() {
		return is_3c;
	}

	public void setIs_3c(final double is_3c) {
		this.is_3c = is_3c;
	}

	public double getMs1_1() {
		return ms1_1;
	}

	public void setMs1_1(final double ms1_1) {
		this.ms1_1 = ms1_1;
	}

	public double getMs1_2a() {
		return ms1_2a;
	}

	public void setMs1_2a(final double ms1_2a) {
		this.ms1_2a = ms1_2a;
	}

	public double getMs1_2b() {
		return ms1_2b;
	}

	public void setMs1_2b(final double ms1_2b) {
		this.ms1_2b = ms1_2b;
	}

	public double getMs1_3a() {
		return ms1_3a;
	}

	public void setMs1_3a(final double ms1_3a) {
		this.ms1_3a = ms1_3a;
	}

	public double getMs1_3b() {
		return ms1_3b;
	}

	public void setMs1_3b(final double ms1_3b) {
		this.ms1_3b = ms1_3b;
	}

	public double getMs1_5a() {
		return ms1_5a;
	}

	public void setMs1_5a(final double ms1_5a) {
		this.ms1_5a = ms1_5a;
	}

	public double getMs1_5b() {
		return ms1_5b;
	}

	public void setMs1_5b(final double ms1_5b) {
		this.ms1_5b = ms1_5b;
	}

	public double getMs1_5c() {
		return ms1_5c;
	}

	public void setMs1_5c(final double ms1_5c) {
		this.ms1_5c = ms1_5c;
	}

	public double getMs1_5d() {
		return ms1_5d;
	}

	public void setMs1_5d(final double ms1_5d) {
		this.ms1_5d = ms1_5d;
	}

	public double getMs2_1() {
		return ms2_1;
	}

	public void setMs2_1(final double ms2_1) {
		this.ms2_1 = ms2_1;
	}

	public double getMs2_2() {
		return ms2_2;
	}

	public void setMs2_2(final double ms2_2) {
		this.ms2_2 = ms2_2;
	}

	public double getMs2_3() {
		return ms2_3;
	}

	public void setMs2_3(final double ms2_3) {
		this.ms2_3 = ms2_3;
	}

	public double getMs2_4a() {
		return ms2_4a;
	}

	public void setMs2_4a(final double ms2_4a) {
		this.ms2_4a = ms2_4a;
	}

	public double getMs2_4b() {
		return ms2_4b;
	}

	public void setMs2_4b(final double ms2_4b) {
		this.ms2_4b = ms2_4b;
	}

	public double getMs2_4c() {
		return ms2_4c;
	}

	public void setMs2_4c(final double ms2_4c) {
		this.ms2_4c = ms2_4c;
	}

	public double getMs2_4d() {
		return ms2_4d;
	}

	public void setMs2_4d(final double ms2_4d) {
		this.ms2_4d = ms2_4d;
	}

	public double getP_1() {
		return p_1;
	}

	public void setP_1(final double p_1) {
		this.p_1 = p_1;
	}

	public double getP_2a() {
		return p_2a;
	}

	public void setP_2a(final double p_2a) {
		this.p_2a = p_2a;
	}

	public double getP_2b() {
		return p_2b;
	}

	public void setP_2b(final double p_2b) {
		this.p_2b = p_2b;
	}

	public double getP_2c() {
		return p_2c;
	}

	public void setP_2c(final double p_2c) {
		this.p_2c = p_2c;
	}

	public double getP_3() {
		return p_3;
	}

	public void setP_3(final double p_3) {
		this.p_3 = p_3;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public String getHiddenReason() {
		return hiddenReason;
	}

	/**
	 * Not serialized to the database. Convenience field only.
	 *
	 * @param hiddenReason Why was this entry hidden.
	 */
	public void setHiddenReason(final String hiddenReason) {
		this.hiddenReason = hiddenReason;
	}

	public int getTransaction() {
		return transaction;
	}

	public void setTransaction(final int transaction) {
		this.transaction = transaction;
	}

	public Map<QuameterProteinGroup, Integer> getIdentifiedSpectra() {
		return identifiedSpectra;
	}

	public void setIdentifiedSpectra(Map<QuameterProteinGroup, Integer> identifiedSpectra) {
		this.identifiedSpectra = identifiedSpectra;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(searchResult, fileSearch, hidden);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuameterResult other = (QuameterResult) obj;
		return Objects.equal(this.searchResult, other.searchResult) && Objects.equal(this.fileSearch, other.fileSearch)
				&& Objects.equal(this.hidden, other.hidden);
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.associationEq("fileSearch", getFileSearch()))
				.add(DaoBase.associationEq("searchResult", getSearchResult()));
	}

}
