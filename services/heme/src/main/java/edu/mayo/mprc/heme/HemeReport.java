package edu.mayo.mprc.heme;

import edu.mayo.mprc.heme.dao.HemeTest;

import java.util.Date;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class HemeReport {
	private List<HemeReportEntry> withinRange;
	private List<HemeReportEntry> haveMassDelta;
	private List<HemeReportEntry> allOthers;
	private String name;
	private Date date;
	private double mass;
	private double massTolerance;

	public HemeReport(final HemeTest hemeTest, final List<HemeReportEntry> withinRange, final List<HemeReportEntry> haveMassDelta, final List<HemeReportEntry> allOthers) {
		this.name = hemeTest.getName();
		this.date = hemeTest.getDate();
		this.mass = hemeTest.getMass();
		this.massTolerance = hemeTest.getMassTolerance();
		this.withinRange = withinRange;
		this.haveMassDelta = haveMassDelta;
		this.allOthers = allOthers;
	}

	public String getName() {
		return name;
	}

	public Date getDate() {
		return date;
	}

	public double getMass() {
		return mass;
	}

	public double getMassTolerance() {
		return massTolerance;
	}

	public List<HemeReportEntry> getWithinRange() {
		return withinRange;
	}

	public List<HemeReportEntry> getHaveMassDelta() {
		return haveMassDelta;
	}

	public List<HemeReportEntry> getAllOthers() {
		return allOthers;
	}

	public boolean isMatch(final ProteinId id) {
		return id.getMass() != null && Math.abs(id.getMass() - getMass()) <= getMassTolerance();
	}
}
