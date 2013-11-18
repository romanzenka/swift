package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Makes sure that for a given config and daemon, all pieces are in place.
 * <p/>
 * Then runs a check
 *
 * @author Roman Zenka
 */
@Component("install-command")
public final class InstallCommand implements SwiftCommand {
	@Override
	public String getDescription() {
		return "Make sure that the daemon is ready to run (creates directories, initializes database, etc.)";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		try {
			final String check = install(environment);
			if (check != null) {
				FileUtilities.err(check);
				return ExitCode.Error;
			}
		} catch (MprcException e) {
			FileUtilities.err(e.getMessage());
			return ExitCode.Error;
		}
		FileUtilities.out("Check passed, Swift is successfully installed");
		return ExitCode.Ok;
	}

	public static String install(final SwiftEnvironment environment) {
		String check = null;
		final File configFile = environment.getConfigFile();
		if (configFile == null) {
			// Config is done standalone
			check = "Please create and provide the configuration file first";
		} else {

			final DaemonConfig config = environment.getDaemonConfig();
			final Daemon daemon = environment.createDaemon(config);

			FileUtilities.out("Installing from configuration file: " + environment.getConfigFile().getAbsolutePath() + " for daemon " + environment.getDaemonConfig().getName());
			final Map<String, String> params = new HashMap<String, String>(0);
			daemon.install(params);

			FileUtilities.out("Checking the installation");
			check = daemon.check();
		}
		return check;
	}
}
