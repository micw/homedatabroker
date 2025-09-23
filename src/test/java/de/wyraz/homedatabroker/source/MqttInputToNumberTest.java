package de.wyraz.homedatabroker.source;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MqttInputToNumberTest {
	
	@Test
	public void testStringToNumber() {
		MQTTSource source=new MQTTSource();
		assertThat(source.parseNumber("3")).isEqualTo(3L);
		assertThat(source.parseNumber("3.1")).isEqualTo(3.1);
	}
	
	@Test
	public void testBooleanToNumber() {
		MQTTSource source=new MQTTSource();
		assertThat(source.parseNumber("false")).isEqualTo(0);
		assertThat(source.parseNumber("true")).isEqualTo(1);
		assertThat(source.parseNumber("falSE")).isEqualTo(0);
		assertThat(source.parseNumber("TRue")).isEqualTo(1);
	}

}
