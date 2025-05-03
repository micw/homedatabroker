package de.wyraz.homedatabroker.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.Test;

public class JSONHelperTests {

	@Test
	public void testParseAndFlattenJSON_valueOnly() {
		assertThat(JSONHelper.parseAndFlattenJSON("'Test'".getBytes()))
			.contains(entry("VALUE", "Test"))
			.hasSize(1);
	}
	
	@Test
	public void testParseAndFlattenJSON_simpleKeys() {
		assertThat(JSONHelper.parseAndFlattenJSON("{'a':'b','c':'d'}".getBytes()))
			.contains(entry("a", "b"),entry("c", "d"))
			.hasSize(2);
	}
	
	@Test
	public void testParseAndFlattenJSON_nestedKeys() {
		assertThat(JSONHelper.parseAndFlattenJSON("{'a':{'b':'c','d':'e'}}".getBytes()))
			.contains(entry("a.b", "c"),entry("a.d", "e"))
			.hasSize(2);
	}

	@Test
	public void testParseAndFlattenJSON_array() {
		assertThat(JSONHelper.parseAndFlattenJSON("{'a':['b','c',{'d':'e'}]}".getBytes()))
			.contains(entry("a.1", "b"),entry("a.2", "c"),entry("a.3.d", "e"))
			.hasSize(3);
	}

	@Test
	public void testParseAndFlattenJSON_arrayOnly() {
		assertThat(JSONHelper.parseAndFlattenJSON("['a','b','c']".getBytes()))
			.contains(entry("1", "a"),entry("2", "b"),entry("3", "c"))
			.hasSize(3);
	}
	
}
