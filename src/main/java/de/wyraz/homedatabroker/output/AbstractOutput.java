package de.wyraz.homedatabroker.output;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.wyraz.homedatabroker.config.AbstractComponent;
import de.wyraz.homedatabroker.metric.MetricRegistry;
import jakarta.annotation.PostConstruct;

public abstract class AbstractOutput<T extends AbstractOutputMetric> extends AbstractComponent implements IMetricOutput<T> {

	@Autowired
	protected MetricRegistry registry;
	
	protected abstract List<T> getMetrics();
	
	@PostConstruct
	protected void subscribeMetrics( ) {
		registry.subscribe(getMetrics(), this);
	}
	
	
	
	
}
