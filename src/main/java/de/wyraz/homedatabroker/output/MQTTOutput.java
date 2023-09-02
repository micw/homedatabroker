package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;

/**
 * Publishes data in "OpenMetrics" format, e.g. to VictoriaMetrics (https://docs.victoriametrics.com/#how-to-import-data-in-prometheus-exposition-format)
 */
public class MQTTOutput extends AbstractOutput<MQTTOutput.MQTTOutputMetric> {

	public static class MQTTOutputMetric extends AbstractOutputMetric {
		@NotEmpty
		protected String topic;
	}

	@NotEmpty
	protected List<MQTTOutputMetric> metrics;

	@NotEmpty
	protected String mqttHost;

	protected boolean mqttTls = false;
	
	protected Integer mqttPort;

	protected String mqttUsername;

	protected String mqttPassword;
	
	protected Mqtt3AsyncClient mqttClient;
	
	@Override
	protected List<MQTTOutputMetric> getMetrics() {
		return metrics;
	}
	
	@PostConstruct
	protected synchronized void connect() throws Exception {
		if (mqttClient==null) {
			
			Mqtt3ClientBuilder builder=Mqtt3Client.builder()
			        .identifier("homedatabroker-"+UUID.randomUUID().toString())
			        .serverHost(mqttHost);
			
			if (mqttPort!=null) {
				builder.serverPort(mqttPort);
			}
			if (mqttTls) {
				builder.sslWithDefaultConfig();
			}
			if (!StringUtils.isAnyBlank(mqttUsername, mqttPassword)) {
				builder.simpleAuth()
					.username(mqttUsername)
					.password(mqttPassword.getBytes())
					.applySimpleAuth();
			}
			
			builder.automaticReconnect()
				.initialDelay(500, TimeUnit.MILLISECONDS)
				.maxDelay(10, TimeUnit.SECONDS)
				.applyAutomaticReconnect();
			
			mqttClient = builder.buildAsync();
			
			log.debug("Connecting to {}",mqttHost);
			
			mqttClient.connect().get(5, TimeUnit.SECONDS);
			
			// FIXME: retry on initial failure
			
			log.info("Connected to {}",mqttHost);
		}
	}
	
	@PostConstruct
	protected void disconnect() {
		if (mqttClient!=null) {
			mqttClient.disconnect();
			mqttClient=null;
		}
	}
	
	
	@Override
	public void publishMetric(MQTTOutputMetric metric, ZonedDateTime time, String name, Number value, String unit) {
		if (value==null) {
			return;
		}
		mqttClient.publishWith().topic(metric.topic).payload(value.toString().getBytes()).send();
	}
	
}
