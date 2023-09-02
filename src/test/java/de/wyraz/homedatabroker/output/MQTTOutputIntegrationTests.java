package de.wyraz.homedatabroker.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import de.wyraz.homedatabroker.metric.MetricRegistry;
import de.wyraz.homedatabroker.output.MQTTOutput.MQTTOutputMetric;
import de.wyraz.homedatabroker.test.MosquittoTestContainer;
import de.wyraz.homedatabroker.test.MqttTestClient;

public class MQTTOutputIntegrationTests {

	@Rule
	public MosquittoTestContainer mqttServer = new MosquittoTestContainer();

	protected MQTTOutput mqttOutput;

	protected MetricRegistry metricRegistry;

	protected MqttTestClient testClient;

	@Before
	public void setup() throws Exception {
		mqttOutput = new MQTTOutput();
		mqttOutput.mqttHost = "127.0.0.1";
		mqttOutput.mqttPort = 18831;
		mqttOutput.metrics = new ArrayList<>();

		{
			MQTTOutputMetric metric = new MQTTOutputMetric();
			metric.source = "test.m1";
			metric.topic = "test/m1";
			mqttOutput.metrics.add(metric);
		}
		mqttOutput.connect();

		metricRegistry = new MetricRegistry();
		mqttOutput.registry = metricRegistry;
		mqttOutput.subscribeMetrics();
	}

	public void teardown() {
		if (mqttOutput != null) {
			mqttOutput.disconnect();
			mqttOutput = null;
		}
		if (testClient != null) {
			testClient.close();
			testClient = null;
		}
	}

	@Test
	public void testMetricPublish() throws Exception {
		
		testClient=new MqttTestClient();
		
		metricRegistry.publish("test", "m1", 1.1d, "A");

		Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> testClient.getMessages().size() > 0);

		assertThat(testClient.getMessages()).containsExactly("" + "test/m1 1.1");
	}

	@Test
	public void testAutoReconnect() throws Exception {

		mqttServer.stop();
		mqttServer.start();
		
		testClient=new MqttTestClient();

		metricRegistry.publish("test", "m1", 1.1d, "A");

		Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> testClient.getMessages().size() > 0);

		assertThat(testClient.getMessages()).containsExactly("" + "test/m1 1.1");
	}

}
