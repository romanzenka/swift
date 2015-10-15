package edu.mayo.mprc.swift.commands;

import com.google.gson.*;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.SavedSearchEngineParameters;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Component("dump-parameters-command")
public final class DumpParameterSet implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(DumpParameterSet.class);

	private ParamsDao paramsDao;

	public ParamsDao getParamsDao() {
		return paramsDao;
	}

	@Resource(name = "paramsDao")
	public void setParamsDao(final ParamsDao paramsDao) {
		this.paramsDao = paramsDao;
	}

	@Override
	public String getDescription() {
		return "Dump a saved parameter set in key/value format";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		initializeDatabase(environment, environment.getSwiftSearcher());
		final String savedParamName = environment.getParameters().get(0);
		if (savedParamName == null) {
			LOGGER.fatal("You need to provide saved parameters name as input to this command");
			return ExitCode.Error;
		}

		final SavedSearchEngineParameters savedSearchEngineParameters;
		final JsonObject allParams;
		getParamsDao().begin();
		try {
			savedSearchEngineParameters = paramsDao.findSavedSearchEngineParameters(savedParamName);
			final SearchEngineParameters parameters = savedSearchEngineParameters.getParameters();

			final GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
			builder.registerTypeAdapter(DateTime.class, new JodaTimeTypeAdapter());
			builder.addSerializationExclusionStrategy(new AnnotationExclusionStrategy());
			final Gson gson = builder.create();

			allParams = new JsonObject();

			for (final ParamName paramName : ParamName.values()) {
				final Object parameter = parameters.getValue(paramName);
				final JsonElement parameterJson = gson.toJsonTree(parameter);
				allParams.add(paramName.getId(), parameterJson);
			}

			getParamsDao().commit();
		} catch (final Exception e) {
			getParamsDao().rollback();
			throw new MprcException(e);
		}
		if (savedSearchEngineParameters == null) {
			LOGGER.fatal("There is no parameter set with name '" + savedParamName + "'");
			return ExitCode.Error;
		}


		final LinkedHashMap<String, String> result = jsonToMap(allParams);
		for (final Map.Entry<String, String> entry : result.entrySet()) {
			FileUtilities.out(entry.getKey().substring(0, entry.getKey().length() - 1) + "\t" + entry.getValue().replaceAll("\n", " "));
		}

		return ExitCode.Ok;
	}

	private LinkedHashMap<String, String> jsonToMap(final JsonElement params) {
		final LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

		if (params.isJsonArray()) {
			final JsonArray item = params.getAsJsonArray();
			for (int i = 0; i < item.size(); i++) {
				addWithPrefix(result, item.get(i), "[" + i + "].");
			}
		} else if (params.isJsonObject()) {
			final JsonObject item = params.getAsJsonObject();
			for (final Map.Entry<String, JsonElement> entry : item.entrySet()) {
				addWithPrefix(result, entry.getValue(), entry.getKey() + '.');
			}
		} else if (params.isJsonNull()) {
			result.put("", "<null>");
		} else if (params.isJsonPrimitive()) {
			result.put("", params.getAsString());
		}

		return result;
	}

	private void addWithPrefix(final LinkedHashMap<String, String> result, final JsonElement element, final String prefix) {
		final LinkedHashMap<String, String> mapped = jsonToMap(element);
		for (final Map.Entry<String, String> entry : mapped.entrySet()) {
			result.put(prefix + entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Initialize the database referenced by given Swift searcher.
	 *
	 * @param environment Swift environment.
	 * @param config      The configuration of the Swift searcher.
	 */
	public static void initializeDatabase(final SwiftEnvironment environment, final SwiftSearcher.Config config) {
		LOGGER.info("Initializing database");
		environment.createResource(config.getDatabase());
		LOGGER.info("Database initialized");
	}
}
