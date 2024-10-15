package de.wyraz.homedatabroker.source;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import de.wyraz.homedatabroker.metric.MetricRegistry;
import de.wyraz.homedatabroker.output.AbstractOutput;
import de.wyraz.homedatabroker.output.AbstractOutputMetric;
import de.wyraz.homedatabroker.source.aggregate.AggregationType;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class AggregationSource extends AbstractSource {

	public static class AggregatedMetricInput extends AbstractOutputMetric {
		
		public AggregatedMetricInput(String source) {
			this.source = source;
		}
		
		@Override
		public String getSource() {
			return super.getSource();
		}
		
		protected Number lastValue;
		protected ZonedDateTime lastValueTs;
		
		public Number getLastValue() {
			return lastValue;
		}
		public ZonedDateTime getLastValueTs() {
			return lastValueTs;
		}
		
		protected void update(Number lastValue, ZonedDateTime lastValueTs) {
			synchronized(this) {
				this.lastValue = lastValue;
				this.lastValueTs = lastValueTs;
			}
			
		}
		
		public Number getValueIfValid(ZonedDateTime expireTs) {
			synchronized(this) {
				if (lastValue==null || lastValueTs==null) {
					return null;
				}
				if (expireTs!=null && expireTs.isAfter(lastValueTs)) {
					return null;
				}
				return lastValue;
			}
		}

	}
	
	public static class AggregatedMetric extends AbstractOutput<AggregatedMetricInput> {
		@NotEmpty
		protected String id;
		
		protected String unit;
		
		protected Duration expireInputsAfter; 
		
		@NotEmpty
		protected List<String> inputMetrics;
		
		@NotNull
		protected AggregationType aggregation;
		
		protected transient List<AggregatedMetricInput> _inputMetrics;
		
		@Override
		public void publishMetric(AggregatedMetricInput metric, ZonedDateTime time, String name, Number value,
				String unit) {
			metric.update(value, time);
		}
		
		@Override
		protected List<AggregatedMetricInput> getMetrics() {
			if (_inputMetrics==null) {
				_inputMetrics = new ArrayList<>();
				for (String metric: inputMetrics) {
					// TODO: verify unit, maybe do unit conversions?
					_inputMetrics.add(new AggregatedMetricInput(metric));
				}
			}
			return _inputMetrics;
		}
		
		public void publish(AggregationSource publisher, ZonedDateTime now) {
			ZonedDateTime expireTs=expireInputsAfter==null?null:ZonedDateTime.now().minus(expireInputsAfter);
			Number value=aggregation.apply(_inputMetrics, expireTs);
			publisher.publishMetric(id, value, unit);
		}
	}
	
	@NotEmpty
	protected List<AggregatedMetric> metrics;
	
	@NotEmpty
	protected String cron;

	@Autowired
	protected TaskScheduler scheduler;

	@Autowired
	protected MetricRegistry registry;
	
	@PostConstruct
	protected void start() {
		
		for (AggregatedMetric metric: metrics) {
			registry.subscribe(metric.getMetrics(), metric);
		}
		
		scheduler.schedule(() -> { schedule(); }, new CronTrigger(cron));
	}
	
	protected void schedule() {
		for (AggregatedMetric metric: metrics) {
			metric.publish(this, ZonedDateTime.now());
		}
	}
	
}
