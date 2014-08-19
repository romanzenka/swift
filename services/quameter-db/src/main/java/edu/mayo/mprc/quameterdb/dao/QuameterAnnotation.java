package edu.mayo.mprc.quameterdb.dao;

import com.google.common.base.Objects;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author Raymond Moore
 */
public class QuameterAnnotation extends PersistableBase {
	private String metricCode;
	private Integer quameterResultId;
	private String text;

	public QuameterAnnotation() {
	}

	/**
	 * Helper - useful in case when you know the QuameterColumn
	 */
	public QuameterAnnotation(final QuameterResult.QuameterColumn column, final Integer quameterResultId, final String text) {
		this(column.name(), quameterResultId, text);
	}

	/**
	 * @param metricCode       Name of the metric. Should correspond to {@link edu.mayo.mprc.quameterdb.dao.QuameterResult.QuameterColumn}
	 * @param quameterResultId Id of the quameter result this annotation pertains to.
	 * @param text             Text of the annotation.
	 */
	public QuameterAnnotation(final String metricCode, final Integer quameterResultId, final String text) {
		this.metricCode = metricCode;
		this.quameterResultId = quameterResultId;
		this.text = text;
	}

	public String getMetricCode() {
		return metricCode;
	}

	public void setMetricCode(final String metricCode) {
		this.metricCode = metricCode;
	}

	public Integer getQuameterResultId() {
		return quameterResultId;
	}

	public void setQuameterResultId(final Integer quameterResultId) {
		this.quameterResultId = quameterResultId;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("metricCode", getMetricCode()))
				.add(DaoBase.nullSafeEq("quameterResultId", getQuameterResultId()));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(metricCode, quameterResultId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuameterAnnotation other = (QuameterAnnotation) obj;
		return Objects.equal(this.metricCode, other.metricCode) && Objects.equal(this.quameterResultId, other.quameterResultId);
	}

	public String prettyPrint() {
		return "Code: " + metricCode + " ResultId: " + quameterResultId + "\nText: " + text;
	}
}
