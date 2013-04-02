package edu.mayo.mprc.swift.configuration.server.properties.validator;

import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.MapConfigWriter;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.FixTag;
import edu.mayo.mprc.config.ui.PropertyChangeListener;
import edu.mayo.mprc.config.ui.UiResponse;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.Map;

/**
 * Validates files or directories.
 */
public final class FileValidator implements PropertyChangeListener {
	private DependencyResolver resolver;
	private boolean mustExist;
	private boolean mustBeDirectory;
	private boolean mustBeWritable;

	public FileValidator(final DependencyResolver resolver, final boolean mustExist, final boolean mustBeDirectory, final boolean mustBeWritable) {
		this.resolver = resolver;
		this.mustExist = mustExist;
		this.mustBeDirectory = mustBeDirectory;
		this.mustBeWritable = mustBeWritable;
	}

	protected String validate(final String filePath) {
		final SecurityManager securityManager = System.getSecurityManager();

		final File file = new File(filePath).getAbsoluteFile();

		if (mustExist && !file.exists()) {
			if (mustBeDirectory) {
				return "Directory <span class=\"file-path\">" + file.getAbsolutePath() + "</span> does not exist. " + FixTag.getTag("createDir", "Create directory");
			} else {
				return "File <span  class=\"file-path\">" + file.getAbsolutePath() + "</span> does not exist.";
			}
		}

		if (mustBeDirectory && !file.isDirectory()) {
			return "File is not a directory <span style=\"file-path\">" + file.getAbsolutePath() + "</span>. Select a different directory to fix this problem.";
		} else if (!mustBeDirectory && file.isDirectory()) {
			return "File is a directory <span style=\"file-path\">" + file.getAbsolutePath() + "</span>. Select a different file that is not a directory to fix this problem.";
		}

		if (mustBeWritable) {
			try {
				securityManager.checkWrite(file.getAbsolutePath());
			} catch (SecurityException e) {
				return "Server does not have write access to file or directory " + file.getAbsolutePath() + ".";
			}
		}

		return null;
	}

	protected void fix(final String filePath) {
		final File file = new File(filePath).getAbsoluteFile();

		if (mustExist && !file.exists()) {
			if (mustBeDirectory) {
				FileUtilities.ensureFolderExists(file);
			}
		}

		if (file.exists() && mustBeWritable) {
			try {
				System.getSecurityManager().checkWrite(file.getAbsolutePath());
			} catch (SecurityException e) {
				file.setWritable(true);
			}
		}
	}

	@Override
	public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
		if (validationRequested) {
			final String error = validate(newValue);
			if (error != null) {
				response.displayPropertyError(config, propertyName, error);
			}
		}
	}

	@Override
	public void fixError(final ResourceConfig config, final String propertyName, final String action) {
		MapConfigWriter writer = new MapConfigWriter(resolver);
		config.save(writer);
		final Map<String, String> map = writer.getMap();
		fix(map.get(propertyName));
	}
}
