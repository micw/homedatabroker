package de.wyraz.homedatabroker.config;

import java.io.File;
import java.util.Arrays;

import org.yaml.snakeyaml.nodes.Node;

public class ConfigurationException extends RuntimeException {
	
	protected File configFile;
	
	protected final Node node;
	
	protected static Object[] extractArgs(Object[] args) {
		if (args.length>0 && (args[args.length-1] instanceof Throwable)) {
			return Arrays.copyOfRange(args, 0, args.length-1);
		}
		return args;
	}
	protected static Throwable extractCause(Object[] args) {
		if (args.length>0 && (args[args.length-1] instanceof Throwable)) {
			return (Throwable) args[args.length-1];
		}
		return null;
	}
	
	protected static String createMessage(Node node, String message, Object... args) {
		StringBuilder msg=new StringBuilder();
		msg.append(String.format(message, extractArgs(args)));
		msg.append(getErrorLocation(node));
		
		return msg.toString();
	}
	
	public ConfigurationException(Node node, String message, Object... args) {
		super(createMessage(node, message, args),extractCause(args));
		this.node=node;
	}
	
	public Node getNode() {
		return node;
	}
	
	public File getConfigFile() {
		return configFile;
	}

	public String getErrorLocation() {
		return getErrorLocation(node);
	}
	
	protected static String getErrorLocation(Node node) {
		StringBuilder sb=new StringBuilder();
		if (node != null) {
			sb
				.append("\n")
				.append(node.getStartMark().toString())
				.append("\n");
		}
		return sb.toString();
	}
	
}
