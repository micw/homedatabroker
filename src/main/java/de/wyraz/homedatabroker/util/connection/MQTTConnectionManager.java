package de.wyraz.homedatabroker.util.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Service;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;

import de.wyraz.homedatabroker.util.connection.AbstractConnectionManager.IConnection;
import de.wyraz.homedatabroker.util.connection.MQTTConnectionManager.MQTTConnection;
import de.wyraz.homedatabroker.util.connection.MQTTConnectionManager.MQTTConnectionParams;
import jakarta.validation.constraints.NotEmpty;

@Service
public class MQTTConnectionManager extends AbstractConnectionManager<MQTTConnection, MQTTConnectionParams> {
	
	@Override
	protected MQTTConnection createNewConnection(MQTTConnectionParams params) {
		return new MQTTConnection(params);
	}
	
	public static class MQTTConnectionParams {
		
		@NotEmpty
		protected String host;

		protected boolean tls = false;
		
		protected Integer port;

		protected String username;

		protected String password;
		
		
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		
		@Override
		public String toString() {
			return "MQTT@"+host+":"+port;
		}

	}
	
	public class MQTTConnection implements IConnection {
		
		protected final MQTTConnectionParams params;

		protected InetAddress hostAddress;
		
		protected MQTTConnection(MQTTConnectionParams params) {
			this.params=params;
		}
		
		protected Mqtt3AsyncClient mqttClient;
		
		public boolean checkConnection() {
			synchronized(this) {
				if (mqttClient==null) {
					try {
						hostAddress=InetAddress.getByName(params.host); // resolve on each reconnect (address may have changed)
					} catch (UnknownHostException ex) {
						if (hostAddress==null) {
							log.warn("Unknown host: {}", params.host);
							return false;
						}
						log.warn("Unknown host: {} - using last address {}", params.host, hostAddress);
					}
					
					Mqtt3ClientBuilder builder=Mqtt3Client.builder()
					        .identifier("homedatabroker-"+UUID.randomUUID().toString())
					        .serverHost(hostAddress);
					
					if (params.port!=null) {
						builder.serverPort(params.port);
					}
					if (params.tls) {
						builder.sslWithDefaultConfig();
					}
					if (!StringUtils.isAnyBlank(params.username, params.password)) {
						builder.simpleAuth()
							.username(params.username)
							.password(params.password.getBytes())
							.applySimpleAuth();
					}
					
					builder.automaticReconnect()
						.initialDelay(500, TimeUnit.MILLISECONDS)
						.maxDelay(10, TimeUnit.SECONDS)
						.applyAutomaticReconnect();
					
					Mqtt3AsyncClient mqttClient = builder.buildAsync();
					
					log.debug("Connecting to {}",params);
					
					try {
						mqttClient.connect().get(5, TimeUnit.SECONDS);
					} catch (Exception ex) {
						log.warn("Unable to connect to {}: {}", params, ex.getMessage(), ex);
						return false;
					}
					
					this.mqttClient=mqttClient;
					log.info("Connected to {}",params);
				}
				
				return true;
			}
		}
		
		protected void tryClose() {
			synchronized(this) {
				if (mqttClient!=null) {
					try {
						mqttClient.disconnect();
					} catch (Exception ex) {
						// ignored
					}
					mqttClient=null;
				}
			}
		}
		
		public void publish(String topic, byte[] payload) {
			synchronized(this) {
				if (mqttClient==null || payload==null) {
					return;
				}
				mqttClient.publishWith().topic(topic).payload(payload).send();
			}
		}
	}

}
