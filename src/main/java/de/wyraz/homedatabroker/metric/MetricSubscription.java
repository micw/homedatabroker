package de.wyraz.homedatabroker.metric;

import java.time.ZonedDateTime;

import de.wyraz.homedatabroker.output.AbstractOutputMetric;
import de.wyraz.homedatabroker.output.IMetricOutput;

public class MetricSubscription {
	
	protected final AbstractOutputMetric metric;
	@SuppressWarnings("rawtypes")
	protected final IMetricOutput output;
	
	@SuppressWarnings("rawtypes")
	public MetricSubscription(AbstractOutputMetric metric, IMetricOutput output) {
		this.metric = metric;
		this.output = output;
	}

	@SuppressWarnings("unchecked")
	public void publishMetric(ZonedDateTime time, String name, Number value, String unit) {
		output.publishMetric(metric, time, name, value, unit);
	}
}
