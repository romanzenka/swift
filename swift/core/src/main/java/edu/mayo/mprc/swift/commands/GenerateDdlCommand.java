package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Component("generate-ddl-command")
public final class GenerateDdlCommand implements SwiftCommand {

	@Resource(name = "databaseFactory")
	private Database.Factory databaseFactory;

	@Override
	public String getDescription() {
		return "Generate DDL script";
	}

	@Override
	public ExitCode run(SwiftEnvironment environment) {
		final List<Database.Config> modulesOfConfigType = environment.getApplicationConfig().getModulesOfConfigType(Database.Config.class);
		final Database database = databaseFactory.create(modulesOfConfigType.get(0), environment.getApplicationConfig().getDependencyResolver());
		FileUtilities.out(database.getDdl());
		return ExitCode.Ok;
	}
}
