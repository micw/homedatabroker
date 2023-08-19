package de.wyraz.homedatabroker.source;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;

/**
 * @author mwyraz
 */
public class DummySource extends AbstractSource {
	
	public static class DummyMetric {
		@NotEmpty
		protected String id;
		
		protected String unit;
		
		protected double minValue = 0d;
		protected double maxValue = 10000d;
		
	}
	
	@NotEmpty
	protected List<DummyMetric> metrics;
	
	@NotEmpty
	protected String cron;

	@Autowired
	protected TaskScheduler scheduler;

	@PostConstruct
	protected void start() {
		scheduler.schedule(() -> { schedule(); }, new CronTrigger(cron));
	}
	
	protected Random rnd=new Random();
	
	protected void schedule() {
		
		for (DummyMetric metric: metrics) {
			publishMetric(metric.id, rnd.nextDouble(metric.minValue, metric.maxValue), metric.unit);
		}
	}
	
}
