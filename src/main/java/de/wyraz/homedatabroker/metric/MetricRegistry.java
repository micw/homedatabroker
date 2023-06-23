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
	
	public void publish(String sourceId, String metricId, Number value, String unit) {
		
		log.debug("{}.{} = {} {}", sourceId, metricId, value, (unit==null)?"":unit);
		
		String key=sourceId+"."+metricId;
		
		ZonedDateTime time=ZonedDateTime.now();
		
		List<MetricSubscription> msubs=subscriptions.get(key);
		if (msubs!=null) {
			for (MetricSubscription msub: msubs) {
				msub.publishMetric(time, key, value, unit);
			}
		}
		
	}
	
	public void subscribe(List<? extends AbstractOutputMetric> metrics, AbstractOutput<?> output) {
		for (AbstractOutputMetric m: metrics) {
			subscriptions.computeIfAbsent(m.getSource(), (k)->new ArrayList<>())
				.add(new MetricSubscription(m, output));
		}
	}
	
}
