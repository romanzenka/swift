package edu.mayo.mprc.config;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.MprcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
class TestMultiFactory implements MultiFactory {
	@Override
	public Map<String, Class<? extends ResourceConfig>> getConfigClasses() {
		return new ImmutableMap.Builder()
				.put("testResource", TestResource.class)
				.put("testResource2", TestResource2.class)
				.put("service", ServiceConfig.class)
				.put("daemon", DaemonConfig.class)
				.put("runner", TestRunnerConfig.class)
				.put("appliaction", ApplicationConfig.class)
				.build();
	}

	@Override
	public ResourceFactory getFactory(Class<? extends ResourceConfig> configClass) {
		return null;
	}

	@Override
	public String getId(Class<? extends ResourceConfig> configClass) {
		if (TestResource.class.isAssignableFrom(configClass)) {
			return "testResource";
		}
		if (TestResource2.class.isAssignableFrom(configClass)) {
			return "testResource2";
		}
		if (ServiceConfig.class.isAssignableFrom(configClass)) {
			return "service";
		}
		if (DaemonConfig.class.isAssignableFrom(configClass)) {
			return "daemon";
		}
		if (RunnerConfig.class.isAssignableFrom(configClass)) {
			return "runner";
		}
		if (ApplicationConfig.class.isAssignableFrom(configClass)) {
			return "application";
		}
		throw new MprcException("unsupported class " + configClass.getName());
	}

	@Override
	public List<String> getSupportedConfigClassNames() {
		return Arrays.asList("testResource", "testResource2", "service", "runner", "application", "daemon");
	}

	@Override
	public List<Class> getSupportedConfigClasses() {
		final ArrayList<Class> list = new ArrayList<Class>();
		list.add(TestResource.class);
		list.add(TestResource2.class);
		list.add(ServiceConfig.class);
		list.add(RunnerConfig.class);
		list.add(DaemonConfig.class);
		list.add(ApplicationConfig.class);
		return list;
	}

	@Override
	public String getUserName(String type) {
		return type.toUpperCase();
	}

	@Override
	public String getUserName(ResourceConfig config) {
		return getUserName(getId(config.getClass()));
	}

	@Override
	public String getUserName(Class<? extends ResourceConfig> clazz) {
		return getUserName(getId(clazz));
	}

	@Override
	public Object getResourceType(String id) {
		return null;
	}

	@Override
	public Class<? extends ResourceConfig> getConfigClass(String type) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object create(ResourceConfig config, DependencyResolver dependencies) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object createSingleton(ResourceConfig config, DependencyResolver dependencies) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
