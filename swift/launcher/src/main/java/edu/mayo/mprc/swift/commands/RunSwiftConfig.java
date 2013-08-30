package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.launcher.Launcher;
import edu.mayo.mprc.swift.ExitCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Component("swiftRunConfigCommand")
public class RunSwiftConfig implements SwiftCommand {
	@Override
	public String getName() {
		return "config";
	}

	@Override
	public String getDescription() {
		return "Run Swift's configuration interface.";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		final Launcher launcher = new Launcher();
		final List<String> parameters = new ArrayList<String>(environment.getParameters());
		parameters.add("--" + Launcher.CONFIG_OPTION);
		final String[] paramArray = new String[parameters.size()];
		parameters.toArray(paramArray);
		return launcher.runLauncher(paramArray, environment);
	}
}
