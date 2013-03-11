package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.EvolvableBase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a class of mass spectrometers that produce a particular set of ion series.
 * This is an immutable class. Do not get fooled by setters - they are for Hibernate use only.
 */
public class Instrument extends EvolvableBase {
	private String name;
	private Set<IonSeries> series;
	private String mascotName;

	public static final Instrument ORBITRAP = new Instrument("Orbi/FT (ESI-FTICR)", getInitialSeriesForNames("b", "y"), "ESI-FTICR");
	public static final Instrument LTQ_ESI_TRAP = new Instrument("LTQ (ESI-TRAP)", getInitialSeriesForNames("b", "y"), "ESI-TRAP");
	public static final Instrument ELECTRON_COLLISION_DISSOCIATION = new Instrument("ECD (FTMS-ECD)", getInitialSeriesForNames("c", "y", "z"), "FTMS-ECD");
	public static final Instrument MALDI_TOF_TOF = new Instrument("4800 (MALDI-TOF-TOF)", getInitialSeriesForNames("a", "b", "y", "d", "v", "w"), "MALDI-TOF-TOF");

	/**
	 * Hibernate only constructor.
	 */
	Instrument() {
	}

	public Instrument(final String name, final Set<IonSeries> series, final String mascotName) {
		if (name == null) {
			throw new MprcException("Name of the instrument has to be specified");
		}
		if (series == null) {
			throw new MprcException("The ion series of the instrument has to be specified");
		}

		setName(name);
		setMascotName(mascotName);
		setSeries(series);
	}

	public String getName() {
		return name;
	}

	private void setName(final String name) {
		this.name = name;
	}

	public String getMascotName() {
		return mascotName;
	}

	public void setMascotName(final String mascotName) {
		this.mascotName = mascotName;
	}

	public Set<IonSeries> getSeries() {
		return series;
	}

	private void setSeries(final Set<IonSeries> series) {
		this.series = series;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Instrument)) {
			return false;
		}

		final Instrument that = (Instrument) o;

		if (!getName().equals(that.getName())) {
			return false;
		}
		if (!getSeries().equals(that.getSeries())) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getName() != null ? getName().hashCode() : 0;
		result = 31 * result + (getSeries() != null ? getSeries().hashCode() : 0);
		return result;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(" (");
		boolean first = true;
		for (final IonSeries is : getSeries()) {
			if (!first) {
				sb.append(", ");
			} else {
				first = false;
			}
			sb.append(is.getName());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Finds an instrument that supports all the ions from a set given a list of instuments.
	 *
	 * @return First instrument from the list that supports given ion series.
	 */
	public static Instrument findInstrumentMatchingSeries(final HashSet<IonSeries> hasseries, final List<Instrument> supportedInstruments) {
		Instrument instrument = null;
		for (final Instrument inst : supportedInstruments) {
			// search for instrument with matching ion series
			int intersect = 0;
			for (final IonSeries is : inst.getSeries()) {
				if (hasseries.contains(is)) {
					intersect++;
				}
			}

			if (intersect == hasseries.size()) {
				instrument = inst;
				break;
			}
		}
		return instrument;
	}

	private static Set<IonSeries> getInitialSeriesForNames(final String... names) {
		final Set<IonSeries> result = new HashSet<IonSeries>(names.length);
		for (final String name : names) {
			boolean found = false;
			for (final IonSeries series : IonSeries.getInitial()) {
				if (series.getName().equals(name)) {
					result.add(series);
					found = true;
					break;
				}
			}
			if (!found) {
				throw new MprcException("Failed to find series of name " + name);
			}
		}
		return result;
	}

	public Instrument copy() {
		final Set<IonSeries> seriesSet = new HashSet<IonSeries>();
		for (final IonSeries series : getSeries()) {
			seriesSet.add(series.copy());
		}
		final Instrument instrument = new Instrument(getName(), seriesSet, getMascotName());
		instrument.setId(getId());
		return instrument;
	}

	/**
	 * @return Initial list of instruments.
	 */
	public static List<Instrument> getInitial() {
		return Arrays.asList(ORBITRAP, LTQ_ESI_TRAP, ELECTRON_COLLISION_DISSOCIATION, MALDI_TOF_TOF);
	}
}
