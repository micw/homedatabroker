package de.wyraz.homedatabroker.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import de.wyraz.homedatabroker.util.OpenmetricsBuilder;
import jakarta.validation.constraints.NotEmpty;

/**
 * Publishes data in "OpenMetrics" format, e.g. to VictoriaMetrics (https://docs.victoriametrics.com/#how-to-import-data-in-prometheus-exposition-format)
 */
public class OpenMetricsPushOutput extends AbstractOutput<OpenMetricsPushOutput.OpenMetricPushOutputMetric> {

	public static class OpenMetricPushOutputMetric extends AbstractOutputMetric {
		@NotEmpty
		protected String name;

		@NotEmpty
		protected Map<String, String> labels;
	}

	@NotEmpty
	protected List<OpenMetricPushOutputMetric> metrics;

	@NotEmpty
	protected String url;
	
	protected String username;
	
	protected String password;
	
	protected final CloseableHttpClient http = HttpClients.createDefault(); 
	
	@Override
	protected List<OpenMetricPushOutputMetric> getMetrics() {
		return metrics;
	}
	
	@Override
	public void publishMetric(OpenMetricPushOutputMetric metric, ZonedDateTime time, String name, Number value,
			String unit) {
		
		OpenmetricsBuilder builder=new OpenmetricsBuilder();
		builder.metric(metric.name).labels(metric.labels).timestamp(time).value(value);
		
		try {
			pushMetrics(builder.build());
			
			if (lastException!=null) {
				lastException=null; // everything is ok
				log.warn("Upload errors resolved");
			}
			
		} catch (Exception ex) {
			if (checkLastException(ex)) {
				log.warn("Failed to push metrics",ex);
			}
		}
	}
	
	public void pushMetrics(String payload) throws IOException {
		if (StringUtils.isBlank(payload)) {
			return;
		}
		
		RequestBuilder post = RequestBuilder.post(url);
		post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));
		if (!StringUtils.isAnyBlank(username, password)) {
			post.addHeader("Authorization","Basic "+Base64.encodeBase64String((username+":"+password).getBytes()));
		}

		try (CloseableHttpResponse resp = http.execute(post.build())) {
			if (resp.getStatusLine().getStatusCode()<200 || resp.getStatusLine().getStatusCode()>299) {
				log.warn("Invalid response from openmetrics push endpoint: {}",resp.getStatusLine());
			}
		} catch (Exception ex) {
			log.warn("Unable to publish data to openmetrics push endpoint",ex);
		}

		
		
	}
	
	
}
