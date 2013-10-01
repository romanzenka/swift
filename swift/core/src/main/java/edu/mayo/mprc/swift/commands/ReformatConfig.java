package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.AppConfigReader;
import edu.mayo.mprc.config.AppConfigWriter;
import edu.mayo.mprc.config.ApplicationConfig;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.PrintWriter;

/**
 * @author Roman Zenka
 */
@Component("reformatConfigCommand")
public final class ReformatConfig implements SwiftCommand {
	private MultiFactory factory;

	@Override
	public String getName() {
		return "reformat-config";
	}

	@Override
	public String getDescription() {
		return "Reformat a configuration file, write result to the screen";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		if (environment.getParameters().size() != 1) {
			throw new MprcException("Missing path to configuration file.\nUsage: swift --run reformat-config <file>");
		}

		final File file = new File(environment.getParameters().get(0));
		final AppConfigReader reader = new AppConfigReader(file, getFactory());
		final ApplicationConfig config;
		try {
			config = reader.load();
		} finally {
			FileUtilities.closeQuietly(reader);
		}

		AppConfigWriter writer = null;
		PrintWriter printWriter = new PrintWriter(System.out);
		try {
			writer = new AppConfigWriter(printWriter, getFactory());
			writer.save(config);
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
}
