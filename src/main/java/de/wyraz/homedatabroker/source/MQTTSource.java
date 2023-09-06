package de.wyraz.homedatabroker.source;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;

import de.wyraz.tibberpulse.sml.SMLDecoder;
import de.wyraz.tibberpulse.sml.SMLMeterData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class MQTTSource extends AbstractSource {

	static Logger log = LoggerFactory.getLogger(MQTTSource.class);
	
	public static enum PayloadExtractor {
		
		RAW {
			@Override
			List<Map<String, String>> extract(byte[] payload) {
				return Collections.singletonList(Collections.singletonMap("VALUE", new String(payload, StandardCharsets.UTF_8)));
			}
		},
		SML {
			@Override
			List<Map<String, String>> extract(byte[] payload) {
				SMLMeterData data;
				try {
					data=SMLDecoder.decode(payload, true);
				} catch (Exception ex) {
					log.debug("Unable to parse SML from response",ex);
					return Collections.emptyList();
				}
				
				List<Map<String,String>> result=new ArrayList<>();
				
				if (data!=null) {
					
					for (SMLMeterData.Reading r: data.getReadings()) {
						Map<String, String> entry=new HashMap<>();
						entry.put("SML:NAME", StringUtils.firstNonBlank(r.getName(),r.getObisCode()));
						entry.put("SML:OBIS", r.getObisCode());
						entry.put("VALUE", r.getValue()==null?null:String.valueOf(r.getValue()));
						entry.put("UNIT", r.getUnit());
						result.add(entry);
					}
				}
				
				return result;
			}
		},
		SML_HEX {
			@Override
			List<Map<String, String>> extract(byte[] payload) {
				try {
					payload=Hex.decodeHex(new String(payload, StandardCharsets.UTF_8));
				} catch (DecoderException ex) {
					log.warn("Unable to decode SML as HEX",ex);
					return Collections.emptyList();
				}
				return SML.extract(payload);
			}
		}
		
		;
		
		abstract List<Map<String,String>> extract(byte[] payload); 
	}
	
	public static class Subscription {
		@NotEmpty
		protected List<String> topics;

		@NotNull
		protected PayloadExtractor extract = PayloadExtractor.RAW;
		
		@NotEmpty
		protected String metric;
		
		protected String unit="$(UNIT)";
		
		@NotEmpty
		protected String value="$(VALUE)";
		
		protected List<Pattern> topicPatterns;
		
		protected void createTopicPatterns() {
			this.topicPatterns=new ArrayList<>();
			for (String topic: topics) {
				this.topicPatterns.add(Pattern.compile(
						topic
							.replaceAll("\\$", "\\\\$")
							.replaceAll("\\+", "[^/]+")
							.replaceAll("/\\#$", "\\$|/.+")
							));
			}
		}
		
		protected synchronized boolean matches(String topic) {
			
			if (this.topicPatterns==null) {
				createTopicPatterns();
			}
			
			for (Pattern p: topicPatterns) {
				if (p.matcher(topic).matches()) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	@NotEmpty
	protected String mqttHost;

	protected boolean mqttTls = false;
	
	protected Integer mqttPort;

	protected String mqttUsername;

	protected String mqttPassword;
	
	protected Mqtt3AsyncClient mqttClient;
	
	@NotEmpty
	protected List<Subscription> subscribe;
	
	
	
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
			
			Set<String> topics=new LinkedHashSet<>();
			for (Subscription sub: subscribe) {
				topics.addAll(sub.topics);
			}
			
			List<Mqtt3Subscription> subscriptions=new ArrayList<>();
			
			for (String topic: topics) {
				subscriptions.add(Mqtt3Subscription.builder().topicFilter(topic).build());
			}
			
			mqttClient.subscribeWith()
				.addSubscriptions(subscriptions)
				.callback((p) -> consume(p.getTopic().toString(), p.getPayloadAsBytes()))
				.send()
				.get(5, TimeUnit.SECONDS);
			
			log.info("Subscribed to {}",topics);
		}
	}
	
	protected void consume(String topic, byte[] payload) {

		for (Subscription sub: subscribe) {
			if (sub.matches(topic)) {
				
				for (Map<String,String> values: sub.extract.extract(payload)) {

					String metricId=replacePlaceholders(sub.metric, topic, values);
					Number value=parseNumber(replacePlaceholders(sub.value, topic, values));
					String unit=replacePlaceholders(sub.unit, topic, values);
					
					// TODO: use timestamp from mqtt if there is a Use-Case for it

					publishMetric(metricId, value, unit);
				}
			}
		}
	}
	
	protected static Pattern placeholderPattern=Pattern.compile("\\$\\(([^\\)]+?)\\)"); 
	protected String replacePlaceholders(String template, String topic, Map<String,String> values) {
		if (template==null) {
			return null;
		}
        StringBuilder result = new StringBuilder();
        Matcher m = placeholderPattern.matcher(template);
        while (m.find()) {
            m.appendReplacement(result, "");
            result.append(replacePlaceholder(m.group(1), m.group(), topic, values));
        }
        m.appendTail(result);

        return result.toString();
    }

	protected String replacePlaceholder(String placeholder, String originalPlaceholder, String topic, Map<String,String> values) {
		
		if (placeholder.startsWith("TOPIC:")) {
			try {
				int pos=Integer.parseInt(placeholder.substring(6))-1;
				String[] topicParts=topic.split("/");
				if (pos>=0 && pos<topicParts.length) {
					return topicParts[pos];
				}
			} catch (RuntimeException ex) {
				ex.printStackTrace();
			}
		}

		if (values.containsKey(placeholder)) {
			return values.get(placeholder);
		}
		
		return "";
	}
	
	protected Number parseNumber(String value) {
		if (value==null) return null;
		try {
			if (value.contains(".")) {
				return Double.parseDouble(value);
			} else {
				return Long.parseLong(value);
			}
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			// TODO: log
		}
		return null;
	}
	protected ZonedDateTime parseTimestamp(String value) {
		if (value!=null) {
			try {
				return ZonedDateTime.parse(value);
			} catch (Exception ex) {
				//ignored
			}
			try {
				return LocalDateTime.parse(value).atZone(ZoneId.systemDefault());
			} catch (Exception ex) {
				//ignored
			}
		}
		
		return ZonedDateTime.now();
	}
	
	
	@PreDestroy
	protected void disconnect() {
		if (mqttClient!=null) {
			mqttClient.disconnect();
			mqttClient=null;
		}
	}
	
}
