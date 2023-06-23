package de.wyraz.homedatabroker.util;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds a metric as described in https://github.com/OpenObservability/OpenMetrics/blob/main/specification/OpenMetrics.md
 * @author mwyraz
 *
 */
public class OpenmetricsBuilder {
	
	protected StringBuilder sb;
	
	public OpenmetricsBuilder() {
		sb=new StringBuilder();
	}
	
	public MetricBuilder metric(String metric) {
		return new MetricBuilder(metric);
	}
	
	protected void appendMetricLine(CharSequence line) {
		if (sb.length()>0) {
			sb.append("\n");
		}
		sb.append(line);
	}
	
	public class MetricBuilder {
		protected StringBuilder sb;
		protected Long timestamp;
		protected boolean hasLabels;
		protected MetricBuilder(String metric) {
			sb=new StringBuilder();
			sb.append(metric);
		}
		
		public MetricBuilder timestamp(ZonedDateTime timestamp) {
			if (timestamp==null) {
				this.timestamp=null;
			} else {
				this.timestamp=timestamp.toEpochSecond();
			}
			return this;
		}

		public MetricBuilder labels(Map<String,String> labels) {
			if (labels!=null) {
				for (Entry<String, String> e: labels.entrySet()) {
					label(e.getKey(),e.getValue());
				}
			}
			return this;
		}
		
		public MetricBuilder label(String key, String value) {
			if (!StringUtils.isAnyBlank(key,value)) {
				if (hasLabels) {
					sb.append(",");
				} else {
					sb.append(" {");
					hasLabels=true;
				}
				sb.append(key);
				sb.append("=\"");
				sb.append(value); // FIXME: proper escaping!
				sb.append("\"");
			}
			return this;
		}
		
		
		public OpenmetricsBuilder value(Number value) {
			if (value!=null) {
				if (hasLabels) {
					sb.append("}");
				}
				sb.append(" ").append(value);
				if (timestamp!=null) {
					sb.append(" ").append(timestamp);
				}
				
				appendMetricLine(sb);
			}
			return OpenmetricsBuilder.this;
		}
	}
	
	public String build() {
		String result=sb.toString();
		sb.setLength(0);
		return result.isEmpty()?null:result;
	}
	
	

}
