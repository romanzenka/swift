package edu.mayo.mprc.swift;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.DatabaseUtilities;
import edu.mayo.mprc.swift.commands.ExitCode;
import edu.mayo.mprc.swift.commands.SwiftCommand;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import org.springframework.stereotype.Component;

/**
 * @author Roman Zenka
 */
@Component("create-test-command")
public final class CreateTestCommand implements SwiftCommand {
	private Daemon daemon;

	@Override
	public String getDescription() {
		return "Internal command to set up the test database";
	}

	@Override
	public ExitCode run(SwiftEnvironment environment) {
		ResourceConfig database = environment.getDaemonConfig().firstResourceOfType(Database.Config.class);
		environment.createResource(database);

		daemon = environment.createDaemon(environment.getDaemonConfig());
		ImmutableMap<String, String> installMap = new ImmutableMap.Builder<String, String>()
				.put("action", DatabaseUtilities.SchemaInitialization.CreateDrop.getValue())
				.put("test", "true")
				.build();

		daemon.install(installMap);

		return ExitCode.Ok;
	}

	public Daemon getDaemon() {
		return daemon;
	}
}
