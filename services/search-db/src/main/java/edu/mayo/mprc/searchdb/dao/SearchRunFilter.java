package edu.mayo.mprc.searchdb.dao;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.utilities.StringUtilities;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This class describes how to filter and sort a list of search runs.
 *
 * @author Roman Zenka
 *         Date: Jun 28, 2007
 */
public final class SearchRunFilter {

	private int userSortOrder;
	private String userWhereClause;
	private int titleSortOrder;
	private String titleWhereClause;
	private int instrumentSortOrder;
	private String commentWhereClause;
	private int commentSortOrder;
	private Collection<String> instrumentWhereClause;
	private String start;
	private String count;
	private Date startDate;
	private Date endDate;
	private boolean showHidden;

	/**
	 * <ul>
	 * <li>-1 if sorted descending</li>
	 * <li>0 not sorted at all</li>
	 * <li>1 sorted ascending</li>
	 * </ul>
	 *
	 * @return How to order users (by name) - ascending, descending or not at all.
	 */
	public int getUserSortOrder() {
		return userSortOrder;
	}

	public void setUserSortOrder(final int userSortOrder) {
		this.userSortOrder = userSortOrder;
	}

	public String getUserWhereClause() {
		return userWhereClause;
	}

	/**
	 * The where clause should be comma separated list of user IDs to be displayed, example 1,2,3,5,10 would display five different users.
	 * Empty or null string denotes no users to display.
	 *
	 * @param userWhereClause Comma separated list of user IDs to be displayed.
	 */
	public void setUserWhereClause(final String userWhereClause) {
		this.userWhereClause = userWhereClause;
	}

	public int getTitleSortOrder() {
		return titleSortOrder;
	}

	public void setTitleSortOrder(final int titleSortOrder) {
		this.titleSortOrder = titleSortOrder;
	}

	public String getTitleWhereClause() {
		return titleWhereClause;
	}

	public void setTitleWhereClause(final String titleWhereClause) {
		this.titleWhereClause = titleWhereClause;
	}

	public int getInstrumentSortOrder() {
		return instrumentSortOrder;
	}

	public void setInstrumentSortOrder(final int instrumentSortOrder) {
		this.instrumentSortOrder = instrumentSortOrder;
	}

	public Collection<String> getInstrumentWhereClause() {
		return instrumentWhereClause;
	}

	public void setInstrumentWhereClause(final Collection<String> instrumentWhereClause) {
		this.instrumentWhereClause = instrumentWhereClause;
	}

	public int getCommentSortOrder() {
		return commentSortOrder;
	}

	public void setCommentSortOrder(final int commentSortOrder) {
		this.commentSortOrder = commentSortOrder;
	}

	public String getCommentWhereClause() {
		return commentWhereClause;
	}

	public void setCommentWhereClause(final String commentWhereClause) {
		this.commentWhereClause = commentWhereClause;
	}

	public void setUserFilter(final String filter) {
		if (filter == null) {
			setUserSortOrder(0);
			return;
		}
		final String[] parts = filter.split(";");
		for (final String part : parts) {
			final String[] keyValue = part.split("=");
			if ("sort".equals(keyValue[0])) {
				setUserSortOrder(Integer.parseInt(keyValue[1]));
			} else if ("filter".equals(keyValue[0]) && keyValue.length >= 2) {
				setUserWhereClause(keyValue[1]);
			}
		}
	}

	public void setTitleFilter(final String filter) {
		if (filter == null) {
			setTitleSortOrder(0);
			return;
		}
		final String[] parts = filter.split(";");
		for (final String part : parts) {
			final String[] keyValue = part.split("=");
			if ("sort".equals(keyValue[0])) {
				setTitleSortOrder(Integer.parseInt(keyValue[1]));
			} else if ("filter".equals(keyValue[0]) && keyValue.length >= 2) {
				setTitleWhereClause(keyValue[1]);
			}
		}
	}

	public void setInstrumentFilter(final String filter) {
		if (filter == null) {
			setInstrumentWhereClause(null);
			return;
		}
		final String[] parts = filter.split(";");
		for (final String part : parts) {
			final String[] keyValue = part.split("=");
			if ("sort".equals(keyValue[0])) {
				setInstrumentSortOrder(Integer.parseInt(keyValue[1]));
			} else if ("filter".equals(keyValue[0]) && keyValue.length >= 2) {
				setInstrumentWhereClause(Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(keyValue[1])));
			}
		}
	}

	public void setCommentFilter(final String filter) {
		if (filter == null) {
			setCommentSortOrder(0);
			return;
		}
		final String[] parts = filter.split(";");
		for (final String part : parts) {
			final String[] keyValue = part.split("=");
			if ("sort".equals(keyValue[0])) {
				setCommentSortOrder(Integer.parseInt(keyValue[1]));
			} else if ("filter".equals(keyValue[0]) && keyValue.length >= 2) {
				setCommentWhereClause(keyValue[1]);
			}
		}
	}

	/**
	 * @return First search run to write in given ordering (0 - start from beginning). Null = not set, start from beginning, too.
	 */
	public String getStart() {
		return start;
	}

	public void setStart(final String start) {
		this.start = start;
	}

	/**
	 * @return How many search runs to write total, if null, write everything.
	 */
	public String getCount() {
		return count;
	}

	public void setCount(final String count) {
		this.count = count;
	}

	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Search run must have started at time greater or equal the given one.
	 */
	public void setStartDate(final Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param showHidden If true, all searches (including the hidden ones) are shown.
	 */
	public void setShowHidden(final boolean showHidden) {
		this.showHidden = showHidden;
	}

	public boolean isShowHidden() {
		return showHidden;
	}

	/**
	 * Search run must have started at time less than or equal the given one.
	 */
	public void setEndDate(final Date endDate) {
		this.endDate = endDate;
	}

	public Criteria makeCriteria(final Session session) {
		final Criteria criteria = session.createCriteria(SearchRun.class);

		if (!StringUtilities.stringEmpty(start)) {
			criteria.setFirstResult(Integer.parseInt(start));
		}
		if (!StringUtilities.stringEmpty(count)) {
			criteria.setMaxResults(Integer.parseInt(count));
		}
		Criteria submittingUserCriteria = null;
		if (!StringUtilities.stringEmpty(userWhereClause)) {
			submittingUserCriteria = criteria.createCriteria("submittingUser");
			submittingUserCriteria.add(Restrictions.in("id", usersWhereClauseAsArray()));
		}
		if (!StringUtilities.stringEmpty(titleWhereClause)) {
			criteria.add(Restrictions.like("title", titleWhereClause, MatchMode.ANYWHERE));
		}
		if (!StringUtilities.stringEmpty(commentWhereClause)) {
			// We do an HQL query to obtain a list of all swift searches whose metadata match
			final Query defIdsMatching = session.createQuery("select distinct sd.id from SwiftSearchDefinition sd where sd.metadata['comment'] like :comment")
					.setParameter("comment", "%" + commentWhereClause + "%");

			final List list = defIdsMatching.list();

			criteria
					.add(Property.forName("swiftSearch").in(list));
		}
		if (instrumentWhereClause != null && !instrumentWhereClause.isEmpty()) {
			final DetachedCriteria matchingAnalysisIds =
					DetachedCriteria.forClass(Analysis.class, "a")
							.createAlias("a.biologicalSamples", "bs")
							.createAlias("bs.list", "bsl")
							.createAlias("bsl.searchResults", "sr")
							.createAlias("sr.list", "srl")
							.createAlias("srl.massSpecSample", "ms")
							.add(Restrictions.in("ms.instrumentSerialNumber", instrumentWhereClause))
							.setProjection(Projections.property("a.id"));
			criteria
					.createAlias("reports", "r")
					.add(Property.forName("r.analysisId").in(matchingAnalysisIds));
		}
		if (startDate != null) {
			criteria.add(Restrictions.isNotNull("startTimestamp"));
			criteria.add(Restrictions.ge("startTimestamp", startDate));
		}
		if (endDate != null) {
			criteria.add(Restrictions.isNotNull("endTimestamp"));
			criteria.add(Restrictions.lt("endTimestamp", endDate));
		}
		if (!isShowHidden()) {
			criteria.add(Restrictions.eq("hidden", 0));
		}
		switch (titleSortOrder) {
			case -1:
				criteria.addOrder(Order.desc("title"));
				break;
			case 1:
				criteria.addOrder(Order.asc("title"));
				break;
			default:
				// No order specified
		}
		switch (userSortOrder) {
			case -1:
				if (submittingUserCriteria == null) {
					submittingUserCriteria = criteria.createCriteria("submittingUser");
				}
				submittingUserCriteria.addOrder(Order.desc("lastName"));
				break;
			case 1:
				if (submittingUserCriteria == null) {
					submittingUserCriteria = criteria.createCriteria("submittingUser");
				}
				submittingUserCriteria.addOrder(Order.asc("lastName"));
				break;
			default:
				criteria.addOrder(Order.desc("startTimestamp"));
				break;
		}
		return criteria;
	}

	private Integer[] usersWhereClauseAsArray() {
		final String[] parts = userWhereClause.split(",");
		final Integer[] ids = new Integer[parts.length];
		for (int i = 0; i < parts.length; i++) {
			final String part = parts[i];
			final int userId = Integer.parseInt(part);
			ids[i] = userId;
		}
		return ids;
	}

	@Override
	public String toString() {
		return "SearchRunFilter{" +
				"userSortOrder=" + userSortOrder +
				", userWhereClause='" + userWhereClause + '\'' +
				", start='" + start + '\'' +
				", count='" + count + '\'' +
				'}';
	}
}
