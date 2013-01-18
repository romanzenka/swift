package edu.mayo.mprc.swift.configuration.server.properties.validator;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.UiResponse;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ExecutableValidator implements PropertyChangeListener {
	private List<String> commandLineParams;

	public ExecutableValidator(final List<String> commandLineParams) {
		if (commandLineParams == null) {
			this.commandLineParams = new ArrayList<String>(0);
		} else {
			this.commandLineParams = commandLineParams;
		}
	}

	@Override
	public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
		if (validationRequested) {
			final ProcessBuilder builder = new ProcessBuilder();
			final File executable = FileUtilities.getAbsoluteFileForExecutables(new File(newValue));
			if(executable.isAbsolute()) {
				builder.directory(executable.getParentFile());
			}
			final List<String> command = new ArrayList<String>(commandLineParams.size() + 1);
			command.add(executable.getPath());
			command.addAll(commandLineParams);
			builder.command(command);
			final ProcessCaller caller = new ProcessCaller(builder);
			try {
				caller.run();
			} catch (Exception t) {
				response.displayPropertyError(config, propertyName, "Problem executing <tt>" + newValue + "</tt>: " + MprcException.getDetailedMessage(t));
				return;
			}
			if (caller.getExitValue() != 0) {
				response.displayPropertyError(config, propertyName,
						"<tt>" + newValue + "</tt> failed with exit code " + caller.getExitValue() +
								"<br/>Output log:<br/><pre>" + caller.getOutputLog() + "</pre><br/>Error log:<br/><pre>" +
								caller.getErrorLog() + "</pre>");
			}
		}
	}

	@Override
	public void fixError(final ResourceConfig config, final String propertyName, final String action) {
		// No way of fixing this
	}
}
