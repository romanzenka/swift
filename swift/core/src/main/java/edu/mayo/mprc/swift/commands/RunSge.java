package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.SgeJobRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author Roman Zenka
 */
@Component("sgeRunCommand")
public class RunSge implements SwiftCommand {
	public static final String COMMAND = "sge";
	private SgeJobRunner swiftSge;

	@Override
	public String getName() {
		return COMMAND;
	}

	@Override
	public String getDescription() {
		return "Internal command to execute a piece of work within SGE";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		final String xmlConfigFilePath = environment.getParameters().get(0);
		swiftSge.run(new File(xmlConfigFilePath));
		return ExitCode.Ok;
	}

	public SgeJobRunner getSwiftSge() {
		return swiftSge;
	}

	@Resource(name = "swiftSge")
	public void setSwiftSge(final SgeJobRunner swiftSge) {
		this.swiftSge = swiftSge;
	}
}
