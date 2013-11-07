package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author Roman Zenka
 */
@Component("check-config-command")
public final class CheckConfigCommand implements SwiftCommand {
	private ServiceFactory serviceFactory;

	@Override
	public String getDescription() {
		return "Make sure the Swift configuration for the current daemon is correct";
	}

	@Override
	public ExitCode run(SwiftEnvironment environment) {
		try {
			final File configFile = environment.getConfigFile();
			if (configFile == null) {
				// Config is done standalone
				FileUtilities.err("Please create and provide the configuration file first");
				return ExitCode.Error;
			}

			final DaemonConfig config = environment.getDaemonConfig();
			FileUtilities.out("Checking configuration file: " + environment.getConfigFile().getAbsolutePath() + " for daemon " + environment.getDaemonConfig().getName());

			getServiceFactory().initialize(environment.getMessageBroker().getBrokerUrl(), config.getName());
			final Daemon daemon = environment.createDaemon(config);
			daemon.check();

		} catch (MprcException e) {
			FileUtilities.err(e.getMessage());
			return ExitCode.Error;
		}
		FileUtilities.out("Check passed");
		return ExitCode.Ok;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	@Resource(name = "serviceFactory")
	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}
}
