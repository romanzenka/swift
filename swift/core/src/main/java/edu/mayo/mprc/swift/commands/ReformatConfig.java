package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.AppConfigReader;
import edu.mayo.mprc.config.AppConfigWriter;
import edu.mayo.mprc.config.ApplicationConfig;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * @author Roman Zenka
 */
@Component("reformat-config-command")
public final class ReformatConfig implements SwiftCommand {
	private MultiFactory factory;
	private ApplicationConfig applicationConfig;

	@Override
	public String getDescription() {
		return "Reformat a configuration file, write result to the screen or another file";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		if (environment.getParameters().size() < 1) {
			throw new MprcException("Missing path to configuration file.\nUsage: reformat-config <config> [<result>]");
		}

		final File file = new File(environment.getParameters().get(0));

		final PrintWriter printWriter;
		if (environment.getParameters().size() >= 2) {
			String pathname = environment.getParameters().get(1);
			try {
				printWriter = new PrintWriter(new File(pathname));
			} catch (FileNotFoundException e) {
				FileUtilities.err("Could not open file for writing: " + pathname + "\n" + MprcException.getDetailedMessage(e));
				return ExitCode.Error;
			}
		} else {
			printWriter = new PrintWriter(System.out);
		}

		final AppConfigReader reader = new AppConfigReader(file, getFactory());
		try {
			reader.load(applicationConfig);
		} finally {
			FileUtilities.closeQuietly(reader);
		}

		AppConfigWriter writer = null;

		try {
			writer = new AppConfigWriter(printWriter, getFactory());
			writer.save(applicationConfig);
		} finally {
			FileUtilities.closeQuietly(writer);
			FileUtilities.closeQuietly(printWriter);
		}
		return ExitCode.Ok;
	}

	public MultiFactory getFactory() {
		return factory;
	}

	@Resource(name = "resourceTable")
	public void setFactory(final MultiFactory factory) {
		this.factory = factory;
	}

	public ApplicationConfig getApplicationConfig() {
		return applicationConfig;
	}

	@Resource(name = "applicationConfig")
	public void setApplicationConfig(ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}
}
