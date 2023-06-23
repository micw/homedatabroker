package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;

public interface IMetricOutput<T extends AbstractOutputMetric> {

	public abstract void publishMetric(T metric, ZonedDateTime time, String name, Number value, String unit);
}
