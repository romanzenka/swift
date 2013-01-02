package edu.mayo.mprc.swift;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.DaemonConnectionFactory;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.database.DatabaseFactory;
import edu.mayo.mprc.dbundeploy.DatabaseUndeployerWorker;
import edu.mayo.mprc.fastadb.FastaDbWorker;
import edu.mayo.mprc.mascot.MascotCache;
import edu.mayo.mprc.mascot.MascotDeploymentService;
import edu.mayo.mprc.mascot.MascotWorker;
import edu.mayo.mprc.mascot.MockMascotDeploymentService;
import edu.mayo.mprc.mgf2mgf.MgfToMgfWorker;
import edu.mayo.mprc.msconvert.MsconvertCache;
import edu.mayo.mprc.msconvert.MsconvertWorker;
import edu.mayo.mprc.msmseval.MSMSEvalWorker;
import edu.mayo.mprc.msmseval.MsmsEvalCache;
import edu.mayo.mprc.myrimatch.MyrimatchCache;
import edu.mayo.mprc.myrimatch.MyrimatchDeploymentService;
import edu.mayo.mprc.myrimatch.MyrimatchWorker;
import edu.mayo.mprc.omssa.OmssaCache;
import edu.mayo.mprc.omssa.OmssaDeploymentService;
import edu.mayo.mprc.omssa.OmssaWorker;
import edu.mayo.mprc.qa.QaWorker;
import edu.mayo.mprc.qa.RAWDumpCache;
import edu.mayo.mprc.qa.RAWDumpWorker;
import edu.mayo.mprc.qstat.QstatDaemonWorker;
import edu.mayo.mprc.raw2mgf.RawToMgfCache;
import edu.mayo.mprc.raw2mgf.RawToMgfWorker;
import edu.mayo.mprc.scaffold.ScaffoldDeploymentService;
import edu.mayo.mprc.scaffold.ScaffoldWorker;
import edu.mayo.mprc.scaffold.report.ScaffoldReportWorker;
import edu.mayo.mprc.scaffold3.Scaffold3DeploymentService;
import edu.mayo.mprc.scaffold3.Scaffold3Worker;
import edu.mayo.mprc.searchdb.SearchDbWorker;
import edu.mayo.mprc.sequest.SequestCache;
import edu.mayo.mprc.sequest.SequestDeploymentService;
import edu.mayo.mprc.sequest.SequestWorker;
import edu.mayo.mprc.sge.GridRunner;
import edu.mayo.mprc.swift.search.DatabaseValidator;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import edu.mayo.mprc.xtandem.XTandemCache;
import edu.mayo.mprc.xtandem.XTandemDeploymentService;
import edu.mayo.mprc.xtandem.XTandemWorker;

import java.lang.reflect.Field;
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

	private ScaffoldDeploymentService.Factory scaffoldDeployerWorkerFactory;
	private DatabaseFactory databaseFactory;
	private MascotDeploymentService.Factory mascotDeployerWorkerFactory;
	private SearchDbWorker.Factory searchDbWorkerFactory;
	private FastaDbWorker.Factory fastaDbWorkerFactory;
	private SimpleRunner.Factory simpleDaemonRunnerFactory;
	private OmssaWorker.Factory omssaWorkerFactory;
	private SequestDeploymentService.Factory sequestDeployerWorkerFactory;
	private SwiftSearcher.Factory swiftSearcherFactory;
	private DatabaseUndeployerWorker.Factory databaseUndeployerFactory;
	private WebUi.Factory webUiFactory;
	private GridRunner.Factory gridDaemonRunnerFactory;
	private DaemonConnectionFactory daemonConnectionFactory;
	private DatabaseValidator databaseValidator;

	public ResourceTable() {
	}

	private void initialize() {
		if (table.size() != 0) {
			return;
		}
		// Searchers
		addWorkerByReflection(MascotWorker.class);
		addWorkerByReflection(SequestWorker.class);
		addWorkerByReflection(XTandemWorker.class);
		addWorker(OmssaWorker.TYPE, OmssaWorker.NAME, OmssaWorker.Config.class, getOmssaWorkerFactory(), new OmssaWorker.Ui(), OmssaWorker.DESC);
		addWorkerByReflection(MyrimatchWorker.class);
		addWorkerByReflection(ScaffoldWorker.class);
		addWorkerByReflection(Scaffold3Worker.class);

		// DB deployers
		addWorker(MascotDeploymentService.TYPE, MascotDeploymentService.NAME, MascotDeploymentService.Config.class, getMascotDeployerWorkerFactory(), new MascotDeploymentService.Ui(), MascotDeploymentService.DESC);
		addWorkerByReflection(MockMascotDeploymentService.class);
		addWorker(SequestDeploymentService.TYPE, SequestDeploymentService.NAME, SequestDeploymentService.Config.class, getSequestDeployerWorkerFactory(), new SequestDeploymentService.Ui(), SequestDeploymentService.DESC);
		addWorkerByReflection(XTandemDeploymentService.class);
		addWorkerByReflection(OmssaDeploymentService.class);
		addWorkerByReflection(MyrimatchDeploymentService.class);
		addWorker(ScaffoldDeploymentService.TYPE, ScaffoldDeploymentService.NAME, ScaffoldDeploymentService.Config.class, getScaffoldDeployerWorkerFactory(), new ScaffoldDeploymentService.Ui(), ScaffoldDeploymentService.DESC);
		addWorkerByReflection(Scaffold3DeploymentService.class);

		// Format converters
		addWorkerByReflection(RawToMgfWorker.class);
		addWorkerByReflection(MsconvertWorker.class);
		addWorkerByReflection(MgfToMgfWorker.class);

		// Special
		addWorkerByReflection(RAWDumpWorker.class);
		addWorkerByReflection(MSMSEvalWorker.class);
		addWorkerByReflection(QaWorker.class);
		addWorkerByReflection(QstatDaemonWorker.class);
		addWorker(SwiftSearcher.TYPE, SwiftSearcher.NAME, SwiftSearcher.Config.class, getSwiftSearcherFactory(), new SwiftSearcher.Ui(getDatabaseValidator()), SwiftSearcher.DESC);
		addWorker(DatabaseUndeployerWorker.TYPE, DatabaseUndeployerWorker.NAME, DatabaseUndeployerWorker.Config.class, getDatabaseUndeployerFactory(), new DatabaseUndeployerWorker.Ui(), DatabaseUndeployerWorker.DESC);
		addWorkerByReflection(ScaffoldReportWorker.class);
		addWorker(SearchDbWorker.TYPE, SearchDbWorker.NAME, SearchDbWorker.Config.class, getSearchDbWorkerFactory(), new SearchDbWorker.Ui(), SearchDbWorker.DESC);
		addWorker(FastaDbWorker.TYPE, FastaDbWorker.NAME, FastaDbWorker.Config.class, getFastaDbWorkerFactory(), new FastaDbWorker.Ui(), FastaDbWorker.DESC);

		// Caches
		addWorkerByReflection(RawToMgfCache.class);
		addWorkerByReflection(MsconvertCache.class);
		addWorkerByReflection(MascotCache.class);
		addWorkerByReflection(SequestCache.class);
		addWorkerByReflection(XTandemCache.class);
		addWorkerByReflection(MyrimatchCache.class);
		addWorkerByReflection(OmssaCache.class);
		addWorkerByReflection(RAWDumpCache.class);
		addWorkerByReflection(MsmsEvalCache.class);

		// Resources
		addResource(DatabaseFactory.TYPE, DatabaseFactory.NAME, DatabaseFactory.Config.class, getDatabaseFactory(), new DatabaseFactory.Ui(), DatabaseFactory.DESC);
		addResource(WebUi.TYPE, WebUi.NAME, WebUi.Config.class, getWebUiFactory(), new WebUi.Ui(), WebUi.DESC);
		addResource(MessageBroker.TYPE, MessageBroker.NAME, MessageBroker.Config.class, new MessageBroker.Factory(), new MessageBroker.Ui(), MessageBroker.DESC);
		addResource(SERVICE, "Service", ServiceConfig.class, getDaemonConnectionFactory(), null, "???");

		// Runners
		addRunner(SimpleRunner.TYPE, SimpleRunner.NAME, SimpleRunner.Config.class, getSimpleDaemonRunnerFactory());
		addRunner(GridRunner.TYPE, GridRunner.NAME, GridRunner.Config.class, getGridDaemonRunnerFactory());
	}

	public void setScaffoldDeployerWorkerFactory(final ScaffoldDeploymentService.Factory scaffoldDeployerWorkerFactory) {
		this.scaffoldDeployerWorkerFactory = scaffoldDeployerWorkerFactory;
	}

	public ScaffoldDeploymentService.Factory getScaffoldDeployerWorkerFactory() {
		return scaffoldDeployerWorkerFactory;
	}

	public void setDatabaseFactory(final DatabaseFactory databaseFactory) {
		this.databaseFactory = databaseFactory;
	}

	public DatabaseFactory getDatabaseFactory() {
		return databaseFactory;
	}

	public void setMascotDeployerWorkerFactory(final MascotDeploymentService.Factory mascotDeployerWorkerFactory) {
		this.mascotDeployerWorkerFactory = mascotDeployerWorkerFactory;
	}

	public MascotDeploymentService.Factory getMascotDeployerWorkerFactory() {
		return mascotDeployerWorkerFactory;
	}

	public SearchDbWorker.Factory getSearchDbWorkerFactory() {
		return searchDbWorkerFactory;
	}

	public void setSearchDbWorkerFactory(final SearchDbWorker.Factory searchDbWorkerFactory) {
		this.searchDbWorkerFactory = searchDbWorkerFactory;
	}

	public FastaDbWorker.Factory getFastaDbWorkerFactory() {
		return fastaDbWorkerFactory;
	}

	public void setFastaDbWorkerFactory(final FastaDbWorker.Factory fastaDbWorkerFactory) {
		this.fastaDbWorkerFactory = fastaDbWorkerFactory;
	}

	public void setSimpleDaemonRunnerFactory(final SimpleRunner.Factory factory) {
		this.simpleDaemonRunnerFactory = factory;
	}

	public SimpleRunner.Factory getSimpleDaemonRunnerFactory() {
		return simpleDaemonRunnerFactory;
	}

	public SwiftSearcher.Factory getSwiftSearcherFactory() {
		return swiftSearcherFactory;
	}

	public void setSwiftSearcherFactory(final SwiftSearcher.Factory swiftSearcherFactory) {
		this.swiftSearcherFactory = swiftSearcherFactory;
	}

	public DatabaseUndeployerWorker.Factory getDatabaseUndeployerFactory() {
		return databaseUndeployerFactory;
	}

	public void setDatabaseUndeployerFactory(final DatabaseUndeployerWorker.Factory databaseUndeployerFactory) {
		this.databaseUndeployerFactory = databaseUndeployerFactory;
	}

	public WebUi.Factory getWebUiFactory() {
		return webUiFactory;
	}

	public void setWebUiFactory(final WebUi.Factory webUiFactory) {
		this.webUiFactory = webUiFactory;
	}

	public GridRunner.Factory getGridDaemonRunnerFactory() {
		return gridDaemonRunnerFactory;
	}

	public void setGridDaemonRunnerFactory(final GridRunner.Factory gridDaemonRunnerFactory) {
		this.gridDaemonRunnerFactory = gridDaemonRunnerFactory;
	}

	private void addToTable(String type, String userName, Class<? extends ResourceConfig> configClass, ResourceFactory<? extends ResourceConfig, ?> factory, ServiceUiFactory uiFactory, String description, ResourceType resourceType) {
		final ResourceInfo info = new ResourceInfo(type, userName, configClass, factory, resourceType, uiFactory, description);
		table.put(type, info);
		tableConfigClass.put(configClass, info);
	}

	private void addResource(final String type, final String userName, final Class<? extends ResourceConfig> configClass, final ResourceFactory<? extends ResourceConfig, ?> factory, final ServiceUiFactory uiFactory, final String description) {
		addToTable(type, userName, configClass, factory, uiFactory, description, ResourceType.Resource);
	}

	private void addWorker(final String type, final String userName, final Class<? extends ResourceConfig> configClass, final WorkerFactoryBase<? extends ResourceConfig> factory,
	                       final ServiceUiFactory uiFactory, final String description) {
		addToTable(type, userName, configClass, factory, uiFactory, description, ResourceType.Worker);
	}

	private void addRunner(final String id, final String userName, final Class<? extends ResourceConfig> configClass, final ResourceFactory<? extends ResourceConfig, ?> factory) {
		addToTable(id, userName, configClass, factory, null, null, ResourceType.Runner);
	}

	private void addWorkerByReflection(final Class<?> workerClass) {
		final String name = getStaticString(workerClass, "NAME");
		final String type = getStaticString(workerClass, "TYPE");
		final String description = getStaticString(workerClass, "DESC");
		final Class<? extends ResourceConfig> config = getSubclass(workerClass, "Config");
		final WorkerFactoryBase factory = (WorkerFactoryBase) getSubclassInstance(workerClass, "Factory");
		final ServiceUiFactory uiFactory = (ServiceUiFactory) getSubclassInstance(workerClass, "Ui");
		addWorker(type, name, config, factory, uiFactory, description);
	}

	private Object getSubclassInstance(final Class parent, final String suffix) {
		try {
			return getSubclass(parent, suffix).newInstance();
		} catch (InstantiationException e) {
			throw new MprcException("Cannot make an instance of subclass " + suffix + " in " + parent.getName(), e);
		} catch (IllegalAccessException e) {
			throw new MprcException("Cannot access subclass " + suffix + " in " + parent.getName(), e);
		}
	}

	private Class getSubclass(final Class parent, final String suffix) {
		final String className = parent.getName() + "$" + suffix;
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new MprcException("Subclass " + suffix + " does not exist in " + parent.getName(), e);
		}
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

	public void setOmssaWorkerFactory(final OmssaWorker.Factory omssaWorkerFactory) {
		this.omssaWorkerFactory = omssaWorkerFactory;
	}

	public OmssaWorker.Factory getOmssaWorkerFactory() {
		return omssaWorkerFactory;
	}

	public void setSequestDeployerWorkerFactory(final SequestDeploymentService.Factory sequestDeployerWorkerFactory) {
		this.sequestDeployerWorkerFactory = sequestDeployerWorkerFactory;
	}

	public SequestDeploymentService.Factory getSequestDeployerWorkerFactory() {
		return sequestDeployerWorkerFactory;
	}

	public DaemonConnectionFactory getDaemonConnectionFactory() {
		return daemonConnectionFactory;
	}

	public void setDaemonConnectionFactory(final DaemonConnectionFactory daemonConnectionFactory) {
		this.daemonConnectionFactory = daemonConnectionFactory;
	}

	public DatabaseValidator getDatabaseValidator() {
		return databaseValidator;
	}

	public void setDatabaseValidator(final DatabaseValidator databaseValidator) {
		this.databaseValidator = databaseValidator;
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
		return info==null ? null : info.getUserName();
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

	private static String getStaticString(final Class<?> clazz, final String fieldName) {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			final Object value = field.get(null);
			if (value instanceof String) {
				return (String) value;
			} else {
				throw new MprcException("The value of the field '" + fieldName + "' is not a string.");
			}
		} catch (NoSuchFieldException e) {
			throw new MprcException("Cannot access field '" + fieldName + "'.", e);
		} catch (IllegalAccessException e) {
			throw new MprcException("Access to field '" + fieldName + "' is not allowed.", e);
		}
	}
}
