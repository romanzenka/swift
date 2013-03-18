package edu.mayo.mprc.swift;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.DaemonConnectionFactory;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.database.DatabaseFactory;
import edu.mayo.mprc.sge.GridRunner;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.*;

/**
 * Multi-factory for all modules that are defined in Swift.
 * <p/>
 * The UI counterparts of the resources are defined in ResourceTableUIs.
 * <p/>
 * TODO: This should discover the modules automatically, current setup is too painful to keep up to date.
 *
 * @author Roman Zenka
 */
public final class ResourceTable extends FactoryBase<ResourceConfig, Object> implements MultiFactory {
	public static final String SERVICE = "service";

	private final Map</*type*/String, ResourceInfo> table = new LinkedHashMap<String, ResourceInfo>();
	// Same as the previous map, just does lookup using the config class.
	private final Map</*configClass*/Class<? extends ResourceConfig>, ResourceInfo> tableConfigClass = new LinkedHashMap<Class<? extends ResourceConfig>, ResourceInfo>();

	private DatabaseFactory databaseFactory;
	private SimpleRunner.Factory simpleDaemonRunnerFactory;
	private WebUi.Factory webUiFactory;
	private GridRunner.Factory gridDaemonRunnerFactory;
	private DaemonConnectionFactory daemonConnectionFactory;

	private Collection<FactoryDescriptor> factoryDescriptors;

	public ResourceTable() {
	}

	private void initialize() {
		if (table.size() != 0) {
			return;
		}

		for (final FactoryDescriptor descriptor : getFactoryDescriptors()) {
			if (descriptor instanceof WorkerFactoryBase) {
				final WorkerFactoryBase<? extends ResourceConfig> base = (WorkerFactoryBase<? extends ResourceConfig>) descriptor;
				addToTable(descriptor, base, ResourceType.Worker);
			} else if (descriptor instanceof ResourceFactory) {
				final ResourceFactory base = (ResourceFactory) descriptor;
				if (RunnerConfig.class.isAssignableFrom(descriptor.getConfigClass())) {
					addToTable(descriptor, base, ResourceType.Runner);
				} else {
					addToTable(descriptor, base, ResourceType.Resource);
				}
			}
		}

		// The daemon connection factory is very special... figure out how to handle it the same way as others
		addToTable(SERVICE, "Service", ServiceConfig.class, getDaemonConnectionFactory(), null, "???", ResourceType.Resource);
	}

	@Resource(name = "databaseFactory")
	public void setDatabaseFactory(final DatabaseFactory databaseFactory) {
		this.databaseFactory = databaseFactory;
	}

	public DatabaseFactory getDatabaseFactory() {
		return databaseFactory;
	}

	@Resource(name = "simpleDaemonRunnerFactory")
	public void setSimpleDaemonRunnerFactory(final SimpleRunner.Factory factory) {
		this.simpleDaemonRunnerFactory = factory;
	}

	public SimpleRunner.Factory getSimpleDaemonRunnerFactory() {
		return simpleDaemonRunnerFactory;
	}

	public WebUi.Factory getWebUiFactory() {
		return webUiFactory;
	}

	@Resource(name = "webUiFactory")
	public void setWebUiFactory(final WebUi.Factory webUiFactory) {
		this.webUiFactory = webUiFactory;
	}

	public GridRunner.Factory getGridDaemonRunnerFactory() {
		return gridDaemonRunnerFactory;
	}

	@Resource(name = "gridDaemonRunnerFactory")
	public void setGridDaemonRunnerFactory(final GridRunner.Factory gridDaemonRunnerFactory) {
		this.gridDaemonRunnerFactory = gridDaemonRunnerFactory;
	}

	public Collection<FactoryDescriptor> getFactoryDescriptors() {
		return factoryDescriptors;
	}

	@Autowired
	public void setFactoryDescriptors(final Collection<FactoryDescriptor> factoryDescriptors) {
		this.factoryDescriptors = factoryDescriptors;
	}

	private void addToTable(String type, String userName, Class<? extends ResourceConfig> configClass, ResourceFactory<? extends ResourceConfig, ?> factory, ServiceUiFactory uiFactory, String description, ResourceType resourceType) {
		final ResourceInfo info = new ResourceInfo(type, userName, configClass, factory, resourceType, uiFactory, description);
		table.put(type, info);
		tableConfigClass.put(configClass, info);
	}

	private void addToTable(
			final FactoryDescriptor descriptor,
			final ResourceFactory<? extends ResourceConfig, ?> factory,
			final ResourceType resourceType) {
		addToTable(descriptor.getType(),
				descriptor.getUserName(),
				descriptor.getConfigClass(),
				factory,
				descriptor.getServiceUiFactory(),
				descriptor.getDescription(),
				resourceType);
	}

	private Map<String, ResourceInfo> getTable() {
		initialize();
		return table;
	}

	private Map<Class<? extends ResourceConfig>, ResourceInfo> getTableConfigClass() {
		initialize();
		return tableConfigClass;
	}

	@Override
	public Map<String/*type*/, Class<? extends ResourceConfig>> getConfigClasses() {
		final Map<String, Class<? extends ResourceConfig>> map = new HashMap<String, Class<? extends ResourceConfig>>(getTable().size());
		for (final ResourceInfo info : getTable().values()) {
			map.put(info.getId(), info.getConfigClass());
		}
		return map;
	}

	private ResourceInfo getByConfigClass(final Class<? extends ResourceConfig> configClass) {
		return getTableConfigClass().get(configClass);
	}

	@Override
	public ResourceFactory getFactory(final Class<? extends ResourceConfig> configClass) {

		final ResourceInfo info = getByConfigClass(configClass);
		if (info != null) {
			return info.getFactory();
		}

		throw new MprcException("Unknown type " + configClass.getName() +
				", supported types are " + Joiner.on(", ").join(getSupportedConfigClassNames()));
	}

	@Override
	public String getId(final Class<? extends ResourceConfig> configClass) {
		final ResourceInfo info = getByConfigClass(configClass);
		if (info != null) {
			return info.getId();
		}
		return null;
	}

	@Override
	public Collection<String> getSupportedConfigClassNames() {
		final List<String> names = new ArrayList<String>(getTable().size());
		for (final ResourceInfo info : getTable().values()) {
			names.add(info.getConfigClass().getName());
		}
		return names;
	}

	@Override
	public Object create(final ResourceConfig config, final DependencyResolver dependencies) {
		return getFactory(config.getClass()).create(config, dependencies);
	}

	@Override
	public Object createSingleton(final ResourceConfig config, final DependencyResolver dependencies) {
		return getFactory(config.getClass()).createSingleton(config, dependencies);
	}

	/**
	 * @return All defined types in this table.
	 */
	public Set<String> getAllTypes() {
		return getTable().keySet();
	}

	public DaemonConnectionFactory getDaemonConnectionFactory() {
		return daemonConnectionFactory;
	}

	@Resource(name = "daemonConnectionFactory")
	public void setDaemonConnectionFactory(final DaemonConnectionFactory daemonConnectionFactory) {
		this.daemonConnectionFactory = daemonConnectionFactory;
	}

	@Override
	public String getUserName(final String type) {
		return getTable().get(type).getUserName();
	}

	/**
	 * Return user-friendly name for the object being created by the particular config class.
	 *
	 * @param config Configuration class.
	 * @return User-friendly name of the class.
	 */
	@Override
	public String getUserName(final ResourceConfig config) {
		final ResourceInfo info = getByConfigClass(config.getClass());
		return info == null ? null : info.getUserName();
	}

	@Override
	public Object getResourceType(final String id) {
		return getTable().get(id).getResourceType();
	}

	public ResourceType getResourceTypeAsType(final String id) {
		final Object resType = getResourceType(id);
		if (!(resType instanceof ResourceTable.ResourceType)) {
			ExceptionUtilities.throwCastException(resType, ResourceTable.ResourceType.class);
			return null;
		}
		return (ResourceTable.ResourceType) resType;
	}

	@Override
	public Class<? extends ResourceConfig> getConfigClass(final String type) {
		return getTable().get(type).getConfigClass();
	}

	public ServiceUiFactory
	getUiFactory(final String type) {
		return getTable().get(type).getUiFactory();
	}

	public String getDescription(final String type) {
		return getTable().get(type).getDescription();
	}

	private static final class ResourceInfo {
		private final String id;
		private final String userName;
		private final Class<? extends ResourceConfig> configClass;
		private final ResourceFactory<?, ?> factory;
		private final ResourceType resourceType;
		private final ServiceUiFactory uiFactory;
		private final String description;

		public ResourceInfo(final String id, final String userName, final Class<? extends ResourceConfig> configClass, final ResourceFactory<? extends ResourceConfig, ?> factory, final ResourceType resourceType, final ServiceUiFactory uiFactory, final String description) {
			this.id = id;
			this.userName = userName;
			this.configClass = configClass;
			this.factory = factory;
			this.resourceType = resourceType;
			this.uiFactory = uiFactory;
			this.description = description;
		}

		public String getId() {
			return id;
		}

		public String getUserName() {
			return userName;
		}

		public Class<? extends ResourceConfig> getConfigClass() {
			return configClass;
		}

		public ResourceFactory<?, ?> getFactory() {
			return factory;
		}

		public ResourceType getResourceType() {
			return resourceType;
		}

		public ServiceUiFactory getUiFactory() {
			return uiFactory;
		}

		public String getDescription() {
			return description;
		}
	}

	public static enum ResourceType {
		Resource,
		Worker,
		Runner
	}
}
