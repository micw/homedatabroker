package de.wyraz.homedatabroker.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ReflectionUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import de.wyraz.homedatabroker.output.ConsoleOutput;
import de.wyraz.homedatabroker.output.MQTTOutput;
import de.wyraz.homedatabroker.output.OpenMetricsPushOutput;
import de.wyraz.homedatabroker.output.VictronDbusGridMeterOutput;
import de.wyraz.homedatabroker.source.DummySource;
import de.wyraz.homedatabroker.source.MQTTSource;
import de.wyraz.homedatabroker.source.ModBusTCPSource;
import de.wyraz.homedatabroker.source.TibberPulseHttpSource;
import de.wyraz.homedatabroker.source.VictronDBusSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ConfigFileParser implements ApplicationContextInitializer<GenericApplicationContext> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected File configFile;

	protected GenericApplicationContext ctx;

	protected static Map<String, Supplier<AbstractComponent>> SOURCE_TYPES = new HashMap<>();
	static {
		SOURCE_TYPES.put("dummy", () -> new DummySource());
		SOURCE_TYPES.put("mqtt", () -> new MQTTSource());
		SOURCE_TYPES.put("modbus-tcp", () -> new ModBusTCPSource());
		SOURCE_TYPES.put("tibber-pulse-http", () -> new TibberPulseHttpSource());
		SOURCE_TYPES.put("victron-dbus", () -> new VictronDBusSource());
	}

	protected static Map<String, Supplier<AbstractComponent>> OUTPUT_TYPES = new HashMap<>();
	static {
		OUTPUT_TYPES.put("console", () -> new ConsoleOutput());
		OUTPUT_TYPES.put("mqtt", () -> new MQTTOutput());
		OUTPUT_TYPES.put("openmetrics", () -> new OpenMetricsPushOutput());
		OUTPUT_TYPES.put("victron-dbus-gridmeter", () -> new VictronDbusGridMeterOutput());
	}

	public static String getNodeType(Node node) {
		return getNodeType(node.getClass());
	}

	public static String getNodeType(Class<? extends Node> nodeClass) {
		if (nodeClass == MappingNode.class) {
			return "map";
		}
		if (nodeClass == SequenceNode.class) {
			return "sequence";
		}
		if (nodeClass == ScalarNode.class) {
			return "scalar";
		}

		return nodeClass.getSimpleName();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Node> T expectNodeType(Node node, Class<T> expected) throws ConfigurationException {
		if (node.getClass() != expected) {
			throw new ConfigurationException(node, "Expected a %s but got a %s", getNodeType(expected),
					getNodeType(node));
		}
		return (T) node;
	}

	public static MappingNode expectMap(Node node) throws ConfigurationException {
		return expectNodeType(node, MappingNode.class);
	}

	public static SequenceNode expectSequence(Node node) throws ConfigurationException {
		return expectNodeType(node, SequenceNode.class);
	}

	public static ScalarNode expectScalar(Node node) throws ConfigurationException {
		return expectNodeType(node, ScalarNode.class);
	}

	public static Node mapValue(MappingNode node, String key, boolean required) throws ConfigurationException {
		for (NodeTuple t : node.getValue()) {
			if (t.getKeyNode() instanceof ScalarNode) {
				if (key.equals(((ScalarNode) t.getKeyNode()).getValue())) {
					return t.getValueNode();
				}
			}
		}
		if (required) {
			throw new ConfigurationException(node, "Missing required key: %s", key);
		}
		return null;
	}

	public static String mapStringValue(MappingNode node, String key, String defaultValue)
			throws ConfigurationException {
		Node value = mapValue(node, key, false);
		if (value == null) {
			return defaultValue;
		}
		return expectScalar(value).getValue();
	}

	public static String mapStringValue(MappingNode node, String key) throws ConfigurationException {
		return expectScalar(mapValue(node, key, true)).getValue();
	}

	public static boolean mapBooleanValue(MappingNode node, String key, boolean defaultValue)
			throws ConfigurationException {
		Node value = mapValue(node, key, false);
		if (value == null) {
			return defaultValue;
		}
		return "true".equals(expectScalar(value).getValue());
	}

	public static Node readYamlConfig(String configFile) throws ConfigurationException {
		return readYamlConfig(new File(configFile));
	}
	
	public static Node readYamlConfig(File configFile) throws ConfigurationException {
		if (!configFile.exists()) {
			throw new ConfigurationException(null, "No such file: "+configFile);
		}
		
		LoaderOptions loadingConfig=new LoaderOptions();
		Resolver resolver = new Resolver();
		
		try (FileReader r = new FileReader(configFile)) {
			
			StreamReader sr=new StreamReader(r);
			try {
				Field f=StreamReader.class.getDeclaredField("name");
				f.setAccessible(true);
				f.set(sr,configFile.getPath());
			} catch (Exception ex) {
				// failed to set the name of the reade - ignored
			}
			
		    Composer composer = new Composer(new ParserImpl(sr, loadingConfig), resolver, loadingConfig);
			Node config=composer.getSingleNode();
			processIncludes(configFile, config);
			return config;
		} catch (IOException ex) {
			throw new ConfigurationException(null, "Unable to read "+configFile, ex);
		}
	}
	
	protected static void processIncludes(File base, Node config) throws ConfigurationException {
		MappingNode configMap = expectMap(config);

		List<NodeTuple> configContent=configMap.getValue();
		
		for (NodeTuple tuple : new ArrayList<>(configContent)) { // iterate on copy, so that the map can be modified
			String key = expectScalar(tuple.getKeyNode()).getValue();
			if (key.equals("includes")) {
				for (Node n : expectSequence(tuple.getValueNode()).getValue()) {
					File file=new File(base.getParentFile(), expectScalar(n).getValue());
					Node include = readYamlConfig(file);
					MappingNode includeMap = expectMap(include);
					
					configContent.addAll(configContent.indexOf(tuple), includeMap.getValue());
				}
				configContent.remove(tuple);
			}
		}
		
	}
	
	@Override
	public void initialize(GenericApplicationContext ctx) {
		this.ctx = ctx;
		this.configFile=new File(ctx.getEnvironment().getProperty("configFile", "config.yaml"));
		try {
			Node config = readYamlConfig(configFile);

			MappingNode configMap = expectMap(config);

			for (NodeTuple tuple : configMap.getValue()) {
				String key = expectScalar(tuple.getKeyNode()).getValue();
				if (key.equals("sources")) {
					initializeSources(ctx, tuple.getValueNode());
				} else if (key.equals("outputs")) {
					initializeOutputs(ctx, tuple.getValueNode());
				} else {
					throw new ConfigurationException(tuple.getKeyNode(), "Unknown configuration key: %s", key);
				}
			}
		} catch (ConfigurationException ex) {
			ex.configFile = configFile;
			throw ex;
		}
	}

	protected void initializeSources(GenericApplicationContext ctx, Node node) throws ConfigurationException {
		for (Node n : expectSequence(node).getValue()) {
			initializeBean(ctx, n, "source", SOURCE_TYPES);
		}
	}

	protected void initializeOutputs(GenericApplicationContext ctx, Node node) throws ConfigurationException {
		for (Node n : expectSequence(node).getValue()) {
			initializeBean(ctx, n, "output", OUTPUT_TYPES);
		}
	}

	protected void initializeBean(GenericApplicationContext ctx, Node node, String idPrefix,
			Map<String, Supplier<AbstractComponent>> typeMap) throws ConfigurationException {

		MappingNode config = expectMap(node);

		String type = mapStringValue(config, "type");
		String id = mapStringValue(config, "id");
		
		boolean enabled= mapBooleanValue(config, "enabled", true);
		if (!enabled) {
			log.debug("Skipping disabled component {} / {}", type, id);
			return;
		}
		log.info("Configuring {} / {}", type, id);

		Supplier<? extends AbstractComponent> supplier = typeMap.get(type);
		if (supplier != null) {
			registerBean(ctx, idPrefix + "." + id, () -> {
				Object bean = supplier.get();
				try {
					configureObject(bean, config, true);
				} catch (ConfigurationException ex) {
					ex.configFile = configFile;
					throw ex;
				}
				return bean;
			});
		} else {
			throw new ConfigurationException(node, "Unknown source type: %s. Known types are: %s", type,
					Strings.join(SOURCE_TYPES.keySet(), ','));
		}

	}

	protected void registerBean(GenericApplicationContext ctx, String name, Supplier<Object> supplier) {
		DefaultListableBeanFactory bf = (DefaultListableBeanFactory) ctx.getBeanFactory();

		bf.registerBeanDefinition(name,
				BeanDefinitionBuilder.genericBeanDefinition(Object.class, supplier).getBeanDefinition());
	}

	public void configureObject(Object target, MappingNode node, boolean ignoreRootObjectKeys) throws ConfigurationException {

		for (NodeTuple t : node.getValue()) {
			Node keyNode = t.getKeyNode();
			String key = expectScalar(t.getKeyNode()).getValue();
			if (ignoreRootObjectKeys && (key.equals("type") || key.equals("enabled"))) {
				continue;
			}

			Field field = ReflectionUtils.findField(target.getClass(), key);
			if (field == null) {
				throw new ConfigurationException(keyNode, "Unexpected configuration key for %s : %s",
						target.getClass().getSimpleName(), key);
			}

			Node valueNode = t.getValueNode();
			Object value = createObject(valueNode, field.getType(), field.getGenericType());

			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, target, value);

		}

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

		for (ConstraintViolation<?> cv : validator.validate(target)) {
			String property = cv.getPropertyPath().toString();
			Node mapValue = mapValue(node, property, false);
			if (mapValue == null) {
				throw new ConfigurationException(node, "'" + property + "' " + cv.getMessage());
			}
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object createObject(Node node, Class<?> type, Type generic) throws ConfigurationException {

		if (type == String.class) {
			return expectScalar(node).getValue();
		}
		if (type == Integer.class) {
			return Integer.parseInt(expectScalar(node).getValue());
		}
		if (type == Boolean.class || type==Boolean.TYPE) {
			return Boolean.parseBoolean(expectScalar(node).getValue());
		}

		if (Enum.class.isAssignableFrom(type)) {
			String value = expectScalar(node).getValue();
			try {
				return Enum.valueOf((Class) type, value);
			} catch (Exception ex) {
				List<String> values = new ArrayList<>();
				for (Object o : type.getEnumConstants()) {
					values.add(((Enum) o).name());
				}
				throw new ConfigurationException(node, "Unknown value for %s. Known values are: %s",
						type.getSimpleName(), Strings.join(values, ','));
			}

		}

		if (type.isAssignableFrom(List.class) && (generic instanceof ParameterizedType)
				&& (((ParameterizedType) generic).getActualTypeArguments()[0]) instanceof Class) {
			
			Class<?> elementClass = (Class) ((ParameterizedType) generic).getActualTypeArguments()[0];
			List<Object> result = new ArrayList<>();
			for (Node n : expectSequence(node).getValue()) {
				Object el = createObject(n, elementClass, elementClass);
				result.add(el);
			}

			return result;
		}

		if (type.isAssignableFrom(Map.class) && (generic instanceof ParameterizedType)
				&& (((ParameterizedType) generic).getActualTypeArguments()[0])==String.class
				&& (((ParameterizedType) generic).getActualTypeArguments()[1])==String.class
				) {
			Map<String,String> result = new LinkedHashMap<>();
			for (NodeTuple nt : expectMap(node).getValue()) {
				result.put(expectScalar(nt.getKeyNode()).getValue(), expectScalar(nt.getValueNode()).getValue());
			}

			return result;
		}
		
		// Arbitrary (potentially nested) Object
		{
			Object el;
			try {
				el = type.getConstructor().newInstance();
			} catch (ReflectiveOperationException ex) {
				throw new ConfigurationException(node, "Unexpected error", ex);
			}
			configureObject(el, expectMap(node), false);
			return el;
		}
	}

}
