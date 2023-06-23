package de.wyraz.homedatabroker.source;

import org.springframework.beans.factory.annotation.Autowired;

import de.wyraz.homedatabroker.config.AbstractComponent;
import de.wyraz.homedatabroker.metric.MetricRegistry;

public abstract class AbstractSource extends AbstractComponent {
	@Autowired
	protected MetricRegistry metricRegistry;
	
	protected void publishMetric(String metricId, Number value, String unit) {
		metricRegistry.publish(id, metricId, value, unit);
		
	}

}
