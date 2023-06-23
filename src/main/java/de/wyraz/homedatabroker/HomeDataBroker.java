package de.wyraz.homedatabroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import de.wyraz.homedatabroker.config.ConfigFileParser;
import de.wyraz.homedatabroker.config.ConfigurationException;

@SpringBootApplication
@EnableScheduling
public class HomeDataBroker {

	protected static final Logger log = LoggerFactory.getLogger(HomeDataBroker.class);

	public static void main(String[] args) throws Exception {
		try {
			new SpringApplicationBuilder(HomeDataBroker.class)
				.initializers(new ConfigFileParser())	
				.run(args);
		} catch (BeanCreationException ex) {
			Throwable th = ex;
			while (th.getCause() != null) {
				th = th.getCause();
			}
			StringBuilder error = new StringBuilder();
			error.append("\n");
			error.append("\n");
			if (th instanceof ConfigurationException) {
				error.append("Application Configuration Error\n");
				error.append("===============================\n");
				error.append("\n");
				error.append(((ConfigurationException) th).getErrorLocation());
				error.append("\n");
			} else {
				error.append("Application startup failed\n");
				error.append("===========================\n");
				error.append("\n");
			}
			error.append(th.getMessage()).append("\n");
			error.append("\n");

			if (log.isErrorEnabled()) {
				log.error(error.toString());
			} else {
				System.err.println(error.toString());
			}
			System.exit(1);
		}
	}

}
