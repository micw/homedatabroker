package de.wyraz.homedatabroker.output;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import de.wyraz.homedatabroker.metric.MetricRegistry;
import de.wyraz.homedatabroker.output.MQTTOutput.MQTTOutputMetric;

public class MQTTOutputIntegrationTests {
	
	@Rule
	public MqttTestContainer mqttServer=new MqttTestContainer();
	
	protected MQTTOutput mqttOutput;
	
	protected MetricRegistry metricRegistry;
	
	protected Mqtt3AsyncClient mqttClient;
	
	protected List<String> messages;
	
	@Before
	public void setup() throws Exception {
		mqttOutput = new MQTTOutput();
		mqttOutput.mqttHost="127.0.0.1";
		mqttOutput.mqttPort=18831;
		mqttOutput.metrics=new ArrayList<>();
		
		{
			MQTTOutputMetric metric=new MQTTOutputMetric();
			metric.source="test.m1";
			metric.topic="test/m1";
			mqttOutput.metrics.add(metric);
		}
		mqttOutput.connect();
		
		metricRegistry=new MetricRegistry();
		mqttOutput.registry=metricRegistry;
		mqttOutput.subscribeMetrics();
		
		mqttClient=Mqtt3Client.builder()
		        .identifier("test-"+UUID.randomUUID().toString())
		        .serverHost("127.0.0.1")
		        .serverPort(18831)
		        .buildAsync();
		
		try  {
			mqttClient.connectWith()
				.send()
				.get(1, TimeUnit.SECONDS);
		} finally {
			System.err.println(mqttServer.getLogs());
			Thread.sleep(1000000);
		}
		
		messages=new ArrayList<>();
		
		mqttClient.subscribeWith()
        	.topicFilter("#")
        	.callback(publish -> {
        		messages.add(publish.getTopic()+" "+new String(publish.getPayloadAsBytes()));
        	})
        	.send()
        	.get(1, TimeUnit.SECONDS);
		
		
	}
	
	public void teardown() {
		if (mqttOutput!=null) {
			mqttOutput.disconnect();
		}
		if (mqttClient!=null) {
			mqttClient.disconnect();
		}
	}
	
	@Test
	public void testMetricPublish() throws Exception {
		metricRegistry.publish("test", "m1", 1.1d, "A");
		
		Thread.sleep(1000);
		System.err.println(messages);
	}

	@Test
	public void testAutoReconnect() throws Exception {
		
		mqttServer.stop();
		
	}

    protected static class MqttTestContainer extends GenericContainer<MqttTestContainer> {

        protected MqttTestContainer() {
            super("library/eclipse-mosquitto:2.0.16");
            withExposedPorts(1883);
            withNetwork(Network.SHARED);
        }
        
        @Override
        protected void configure() {
        	super.configure();
        	addFixedExposedPort(18831, 1883);
        }
    }
}
