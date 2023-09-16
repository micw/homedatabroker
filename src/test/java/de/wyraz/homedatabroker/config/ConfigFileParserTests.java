package de.wyraz.homedatabroker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;

public class ConfigFileParserTests {
	
	static String toYaml(Node node) throws IOException {
		StringWriter w=new StringWriter();
		new Yaml().serialize(node, w);
		return w.toString().trim();
	}
	
	@Test
	public void testConfigNotFound() throws Exception {
		assertThatCode(()->ConfigFileParser.readYamlConfig("testdata/simple_nonexistant.yaml"))
			.isInstanceOf(ConfigurationException.class)
			.hasMessageContaining("testdata/simple_nonexistant.yaml");
	}
	
	@Test
	public void testLoadSimpleYaml() throws Exception {
		Node node=ConfigFileParser.readYamlConfig("testdata/simple.yaml");
		assertThat(node).isNotNull();
		
		String yaml=toYaml(node);
		
		assertThat(yaml).isEqualTo("a:\n"
				+ "- entry one of a\n"
				+ "- entry two of a\n"
				+ "b:\n"
				+ "- entry one of b");
	}
	
	@Test
	public void testMultipleIncludes() throws Exception {
		Node node=ConfigFileParser.readYamlConfig("testdata/include_base1.yaml");
		assertThat(node).isNotNull();
		
		String yaml=toYaml(node);
		
		assertThat(yaml).isEqualTo("sources:\n"
				+ "- one from includes/include1.yaml\n"
				+ "- two from includes/include1.yaml\n"
				+ "sources:\n"
				+ "- one from base1\n"
				+ "sources:\n"
				+ "- one from includes/include2.yaml\n"
				+ "- two from includes/include2.yaml");
	}

	@Test
	public void testIncludeMultipleFiles() throws Exception {
		Node node=ConfigFileParser.readYamlConfig("testdata/include_base2.yaml");
		assertThat(node).isNotNull();
		
		String yaml=toYaml(node);
		System.err.println(yaml);
		
		assertThat(yaml).isEqualTo("sources:\n"
				+ "- one from includes/include1.yaml\n"
				+ "- two from includes/include1.yaml\n"
				+ "sources:\n"
				+ "- one from includes/include2.yaml\n"
				+ "- two from includes/include2.yaml\n"
				+ "sources:\n"
				+ "- one from base1");
	}
	
	@Test
	public void testRootYamlError() throws Exception {
		assertThatCode(()->ConfigFileParser.readYamlConfig("testdata/simple_error.yaml"))
			.isInstanceOf(ConfigurationException.class)
			.hasMessageContaining("in testdata/simple_error.yaml, line 1, column 1:");
	}

	@Test
	public void testIncludedYamlError() throws Exception {
		
		assertThatCode(()->ConfigFileParser.readYamlConfig("testdata/include_error.yaml"))
			.isInstanceOf(ConfigurationException.class)
			.hasMessageContaining("in testdata/simple_error.yaml, line 1, column 1:");
	}
	
}
