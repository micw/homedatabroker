package de.wyraz.homedatabroker.source.aggregate;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import de.wyraz.homedatabroker.source.AggregationSource.AggregatedMetricInput;

public enum AggregationType {
	
	sum {
		@Override
		public Number apply(List<AggregatedMetricInput> inputs, ZonedDateTime expireTs, boolean requireAllInputs) {
			BigDecimal result=BigDecimal.ZERO;
			int validCount = 0;
			
			for (AggregatedMetricInput input: inputs) {
				Number value=input.getValueIfValid(expireTs);
				if (value!=null) {
					result=result.add(new BigDecimal(value.doubleValue()));
					validCount++;
				}
			}
			
			// Wenn requireAllInputs aktiv ist und nicht alle Inputs valid sind
			if (requireAllInputs && validCount < inputs.size()) {
				return null;
			}
			
			return result;
		}
	},
	
	;
	
	public abstract Number apply(List<AggregatedMetricInput> inputs, ZonedDateTime expireTs, boolean requireAllInputs);
}
