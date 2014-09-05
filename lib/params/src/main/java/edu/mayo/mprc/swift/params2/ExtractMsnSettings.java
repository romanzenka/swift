package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Settings for raw->mgf convertor.
 */
public class ExtractMsnSettings extends PersistableBase {

	private String commandLineSwitches;
	private String command; // extract_msn or msconvert?

	public static final String EXTRACT_MSN = "extract_msn";
	public static final String MSCONVERT = "msconvert";
	public static final String MZML_MODE = "--mzML";
	public static final String MS2_MODE = "--ms2";

	public ExtractMsnSettings() {
	}

	public static ExtractMsnSettings getMsconvertSettings() {
		return new ExtractMsnSettings("", MSCONVERT);
	}

	public static ExtractMsnSettings getExtractMsnSettings() {
		return new ExtractMsnSettings("-E100 -S1 -I10 -G1", EXTRACT_MSN);
	}

	public static ExtractMsnSettings getDefaultExtractMsnSettings() {
		return getMsconvertSettings();
	}

	public ExtractMsnSettings(final String commandLineSwitches, final String command) {
		this.commandLineSwitches = commandLineSwitches;
		this.command = command;
	}

	public String getCommandLineSwitches() {
		return commandLineSwitches;
	}

	public void setCommandLineSwitches(final String commandLineSwitches) {
		this.commandLineSwitches = commandLineSwitches;
	}

	public String getCommand() {
		return command == null ? EXTRACT_MSN : command;
	}

	public void setCommand(String command) {
		this.command = command == null ? EXTRACT_MSN : command;
	}

	/**
	 * @return True if the command line indicates we should produce mzML file.
	 */
	public boolean isMzMlMode() {
		return getCommandLineSwitches().contains(MZML_MODE);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ExtractMsnSettings)) {
			return false;
		}

		final ExtractMsnSettings that = (ExtractMsnSettings) o;

		if (getCommandLineSwitches() != null ? !getCommandLineSwitches().equals(that.getCommandLineSwitches()) : that.getCommandLineSwitches() != null) {
			return false;
		}
		if (!getCommand().equals(that.getCommand())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = commandLineSwitches != null ? commandLineSwitches.hashCode() : 0;
		result = 31 * result + (command != null ? command.hashCode() : 0);
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("commandLineSwitches", getCommandLineSwitches()))
				.add(DaoBase.nullSafeEq("command", getCommand()));
	}


	public ExtractMsnSettings copy() {
		final ExtractMsnSettings msnSettings = new ExtractMsnSettings(getCommandLineSwitches(), getCommand());
		msnSettings.setId(getId());
		return msnSettings;
	}
}
