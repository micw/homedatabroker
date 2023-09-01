package de.wyraz.homedatabroker.source;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;

public abstract class AbstractScheduledSource extends AbstractSource {

	@NotEmpty
	protected String cron;
	
	@Autowired
	protected TaskScheduler scheduler;

	@PostConstruct
	protected void start() {
		scheduler.schedule(() -> { schedule(); }, new CronTrigger(cron));
	}
	
	protected abstract void schedule();
}
