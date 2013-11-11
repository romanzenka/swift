package edu.mayo.mprc.utilities.testing;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.DatabaseUtilities;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.commands.SwiftCommand;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.search.DatabaseValidator;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Roman Zenka
 */
@Component("create-test-command")
public final class CreateTestCommand implements SwiftCommand {
	private DatabaseValidator validator;
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

		// TODO: Database needs to depend on its validator for its installation needs
		// That way the install runs directly through the daemon
		validator.install(installMap);
		daemon.install(installMap);

		return ExitCode.Ok;
	}

	public DatabaseValidator getValidator() {
		return validator;
	}

	@Resource(name = "databaseValidator")
	public void setValidator(final DatabaseValidator validator) {
		this.validator = validator;
	}

	public Daemon getDaemon() {
		return daemon;
	}
}
