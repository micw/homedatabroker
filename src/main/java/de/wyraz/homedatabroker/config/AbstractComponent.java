package de.wyraz.homedatabroker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.wyraz.homedatabroker.metric.MetricRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;

public abstract class AbstractComponent {
	protected Logger log = LoggerFactory.getLogger(getClass());

	protected boolean enabled = true;

	@NotEmpty
	protected String id;

	@Autowired
	protected MetricRegistry metricRegistry;
	
	protected Exception lastException;
	
	@PostConstruct
	protected void setupLogger() {
		log = LoggerFactory.getLogger(getClass().getName()+"."+id);
	}
	
	protected String getCanonicalExceptionMessage(Exception exception) {
		return exception.getMessage();
	}
	
	protected boolean checkLastException(Exception newException) {
		if (lastException==null || lastException.getClass()!=newException.getClass() ||
				!getCanonicalExceptionMessage(lastException).equals(getCanonicalExceptionMessage(newException))) {
			lastException=newException;
			return false;
		}
		return true;
	}

}
