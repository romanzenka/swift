package edu.mayo.mprc.heme;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * A class holding configuration of the heme UI.
 *
 * @author Roman Zenka
 */
public final class HemeUi {
	public static final String TYPE = "hemeUi";
	public static final String NAME = "HemePathology User Interface";
	public static final String DESC = "Specialized interface for Heme Pathology";

	/**
	 * Where do the test data go
	 */
	private static final String DATA_PATH = "dataPath";

	/**
	 * Where do the test results go
	 */
	private static final String RESULT_PATH = "resultPath";

	private final File data;
	private final File results;

	public HemeUi(final File data, final File results) {
		this.data = data;
		this.results = results;
	}

	public File getData() {
		return data;
	}

	public File getResults() {
		return results;
	}

	@Component("hemeUiFactory")
	public static final class Factory extends FactoryBase<Config, HemeUi> implements FactoryDescriptor {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public String getUserName() {
			return NAME;
		}

		@Override
		public String getDescription() {
			return DESC;
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return new Ui();
		}

		@Override
		public HemeUi create(final Config config, final DependencyResolver dependencies) {
			final File dataDir = new File(config.getDataPath());
			FileUtilities.ensureFolderExists(dataDir);
			final File resultDir = new File(config.getResultPath());
			FileUtilities.ensureFolderExists(resultDir);
			return new HemeUi(dataDir, resultDir);
		}
	}

	public static final class Config implements ResourceConfig {
		private String dataPath;
		private String resultPath;

		public String getDataPath() {
			return dataPath;
		}

		public String getResultPath() {
			return resultPath;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(DATA_PATH, dataPath, "Where to take data from");
			writer.put(RESULT_PATH, resultPath, "Where to put results");
		}

		@Override
		public void load(final ConfigReader reader) {
			dataPath = reader.get(DATA_PATH);
			resultPath = reader.get(RESULT_PATH);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(DATA_PATH, "Data path", "Folder containing the heme pathology test data. Every sub-folder in this folder will be displayed")
					.required()
					.existingDirectory()
					.defaultValue("data")

					.property(RESULT_PATH, "Result path", "Folder where the search results will be stored")
					.required()
					.existingDirectory()
					.defaultValue("results");
		}
	}

}
