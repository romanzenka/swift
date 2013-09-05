package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.launcher.Launcher;
import edu.mayo.mprc.swift.ExitCode;
import org.springframework.stereotype.Component;

/**
 * @author Roman Zenka
 */
@Component("swiftRunWebCommand")
public class RunSwiftWeb implements SwiftCommand {
	@Override
	public String getName() {
		return "web";
	}

	@Override
	public String getDescription() {
		return "Run Swift's web interface.";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		final Launcher launcher = new Launcher();
		return launcher.runLauncher(false, environment);
	}
}
