package de.wyraz.homedatabroker.output;

import jakarta.validation.constraints.NotEmpty;

public abstract class AbstractOutputMetric {
	@NotEmpty
	protected String source;
	
	public String getSource() {
		return source;
	}
}
