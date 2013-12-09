package edu.mayo.mprc.swift.commands;

import org.springframework.stereotype.Component;

/**
 * @author Roman Zenka
 */
@Component("config-command")
public class RunSwiftConfig implements SwiftCommand {
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
