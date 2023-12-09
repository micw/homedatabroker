package de.wyraz.homedatabroker.web;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wyraz.homedatabroker.metric.MetricHolder;
import de.wyraz.homedatabroker.metric.MetricRegistry;
import de.wyraz.homedatabroker.util.OpenmetricsBuilder;

@RestController
public class MetricsController {
	
	@Autowired
	protected MetricRegistry registry;
	
	@RequestMapping(path = "/rest/metrics/sources")
	public ResponseEntity<Collection<MetricHolder>> getRestMetrics() {
		return ResponseEntity.ok(registry.getMetrics().values());
	}
	
	@RequestMapping(path = "/metrics/sources", produces = "application/openmetrics-text; version=1.0.0; charset=utf-8")
	public ResponseEntity<byte[]> getPrometheusSourceMetrics() {
		OpenmetricsBuilder builder=new OpenmetricsBuilder();
		for (Entry<String,MetricHolder> e: registry.getMetrics().entrySet()) {
			builder.metric(e.getKey().replace(".", "_"))
				.timestamp(e.getValue().getLastUpdated())
				.value(e.getValue().getValue());
		}
		
		return ResponseEntity.ok(builder.build().getBytes(StandardCharsets.UTF_8));
	}
	

}
