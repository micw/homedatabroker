package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public class ConsoleOutput extends AbstractOutput<ConsoleOutput.ConsoleOutputMetric> {

	public static class ConsoleOutputMetric extends AbstractOutputMetric {
	}

	@NotEmpty
	protected List<ConsoleOutputMetric> metrics;

	@Override
	protected List<ConsoleOutputMetric> getMetrics() {
		return metrics;
	}

	@Override
	public void publishMetric(ConsoleOutputMetric metric, ZonedDateTime time, String name, Number value, String unit) {
		System.out.println(time + " " + name + " = " + value + " " + unit);
	}

}
