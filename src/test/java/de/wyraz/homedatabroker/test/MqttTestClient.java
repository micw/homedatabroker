package de.wyraz.homedatabroker.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;


public class MqttTestClient {
	
	protected Mqtt3AsyncClient mqttClient;
	
	protected List<String> messages=new ArrayList<>();

	public MqttTestClient() throws Exception {
		
		mqttClient=Mqtt3Client.builder()
		        .identifier("test-"+UUID.randomUUID().toString())
		        .serverHost("127.0.0.1")
		        .serverPort(18831)
		        .buildAsync();
		
		mqttClient.connectWith()
			.send()
			.get(1, TimeUnit.SECONDS);
		
		mqttClient.subscribeWith()
        	.topicFilter("#")
        	.callback(publish -> {
        		messages.add(publish.getTopic()+" "+new String(publish.getPayloadAsBytes()));
        	})
        	.send()
        	.get(1, TimeUnit.SECONDS);
	}
	
	public void close() {
		mqttClient.disconnect();
	}

	public List<String> getMessages() {
		return messages;
	};
}
