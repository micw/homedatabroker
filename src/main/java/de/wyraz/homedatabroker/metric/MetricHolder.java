package de.wyraz.homedatabroker.metric;

import java.time.ZonedDateTime;

public class MetricHolder {
	protected final String sourceId;
	protected final String metricId;
	protected int subscriptionCount;
	protected ZonedDateTime lastUpdated;
	protected Number value;
	protected String unit;

	public MetricHolder(String sourceId, String metricId) {
		this.sourceId = sourceId;
		this.metricId = metricId;
	}
	
	public void countUpSubscription() {
		subscriptionCount++;
	}

	public ZonedDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(ZonedDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Number getValue() {
		return value;
	}

	public void setValue(Number value) {
		this.value = value;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getSourceId() {
		return sourceId;
	}

	public String getMetricId() {
		return metricId;
	}
	
	public int getSubscriptionCount() {
		return subscriptionCount;
	}
	
	
}
