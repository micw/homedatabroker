package de.wyraz.homedatabroker.metric;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.wyraz.homedatabroker.output.AbstractOutput;
import de.wyraz.homedatabroker.output.AbstractOutputMetric;

@Service
public class MetricRegistry {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	protected Map<String, List<MetricSubscription>> subscriptions=new HashMap<>();
	
	protected Map<String, MetricHolder> metrics = new HashMap<>();
	
	public void publish(String sourceId, String metricId, Number value, String unit) {
		publish(ZonedDateTime.now(), sourceId, metricId, value, unit);
	}
	public void publish(ZonedDateTime time, String sourceId, String metricId, Number value, String unit) {
		
		log.debug("{}.{} = {} {}", sourceId, metricId, value, (unit==null)?"":unit);
		
		String key=sourceId+"."+metricId;
		
		MetricHolder holder=getMetricHolder(key);
		holder.setLastUpdated(time);
		holder.setUnit(unit);
		holder.setValue(value);
		
		List<MetricSubscription> msubs=subscriptions.get(key);
		if (msubs!=null) {
			for (MetricSubscription msub: msubs) {
				msub.publishMetric(time, key, value, unit);
			}
		}
	}
	
	protected MetricHolder getMetricHolder(String key) {
		return metrics.computeIfAbsent(key, (k) -> {
			String[] parts=key.split("\\.",2);
			return new MetricHolder(parts[0],parts[1]);
		});
	}
	
	public void subscribe(List<? extends AbstractOutputMetric> metrics, AbstractOutput<?> output) {
		for (AbstractOutputMetric m: metrics) {
			subscriptions.computeIfAbsent(m.getSource(), (k)->new ArrayList<>())
				.add(new MetricSubscription(m, output));
			getMetricHolder(m.getSource()).countUpSubscription();
		}
	}
	
	public Map<String, MetricHolder> getMetrics() {
		return metrics;
	}
	
}
