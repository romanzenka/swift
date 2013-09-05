package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.launcher.Launcher;
import edu.mayo.mprc.swift.ExitCode;
import org.springframework.stereotype.Component;

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
		return launcher.runLauncher(true, environment);
	}
}
