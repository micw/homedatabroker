package de.wyraz.homedatabroker.util.connection;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class ModBusTCPConnectionParams {
	
	@NotEmpty
	protected String host;
	
	@NotNull
	protected Integer port=509;
	
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	
	@Override
	public String toString() {
		return "ModBusTCP@"+host+":"+port;
	}

}
