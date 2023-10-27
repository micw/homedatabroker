package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.SimpleDBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import de.wyraz.homedatabroker.output.VictronDbusGridMeterOutput.VictronDbusOutputMetric;
import de.wyraz.homedatabroker.util.connection.MQTTConnectionManager;
import de.wyraz.homedatabroker.util.connection.MQTTConnectionManager.MQTTConnection;
import de.wyraz.homedatabroker.util.connection.MQTTConnectionManager.MQTTConnectionParams;
import de.wyraz.homedatabroker.util.vedbus.DBusVariant;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * https://github.com/mr-manuel/venus-os_dbus-mqtt-grid
 */
public class VictronMQTTGridMeterOutput extends AbstractOutput<VictronMQTTGridMeterOutput.VictronMQTTOutputMetric> {

	public static class VictronMQTTOutputMetric extends AbstractOutputMetric {
		
		@NotNull
		protected GridValue target;
	}
	
	protected static class ValueHolder {
		protected ZonedDateTime time;
		protected GridValue target;
		protected Number value;
	}
	
	public static enum GridValue {
		
		// TODO: Energy requires unit conversion (Wh -> kWh)
		//AC_ENERGY_FORWARD("grid/energy_forward"),
		//AC_ENERGY_REVERSE("grid/energy_reverse"),
		
		
		AC_POWER("grid/power"),
		
		AC_L1_VOLTAGE("grid/L1/voltage"),
		AC_L1_CURRENT("grid/L1/current"),
		AC_L1_POWER("grid/L1/power"),
		
		AC_L2_VOLTAGE("grid/L2/voltage"),
		AC_L2_CURRENT("grid/L2/current"),
		AC_L2_POWER("grid/L2/power"),
		
		AC_L3_VOLTAGE("grid/L3/voltage"),
		AC_L3_CURRENT("grid/L3/current"),
		AC_L3_POWER("grid/L3/power"),
		;
		
		protected final String[] jsonPath;
		
		private GridValue(String jsonPath) {
			this.jsonPath = jsonPath.split("/");
		}
	}

	
	protected MQTTConnection mqtt;
	
	@Autowired
	protected MQTTConnectionManager mqttManager;
	
	@NotNull
	protected MQTTConnectionParams connection;
	
	@NotEmpty
	protected String topic;
	
	@NotEmpty
	protected List<VictronMQTTOutputMetric> metrics;
	
	protected int expireAfterSeconds = 15;

	@Override
	protected List<VictronMQTTOutputMetric> getMetrics() {
		return metrics;
	}
	
	protected DBusConnection dbusCon;
	
	protected Map<GridValue, ValueHolder> values=new ConcurrentHashMap<>();
	
	@PostConstruct
	protected void start() throws Exception {
		for (GridValue gv: GridValue.values()) {
			ValueHolder vh=new ValueHolder();
			vh.value = null;
			vh.target = gv;
			values.put(gv, vh);
		}
		
		mqtt=mqttManager.getConnection(connection);
	}
	
	@Scheduled(cron = "*/5 * * * * *")
	protected void expireValues() {
		if (metrics==null || expireAfterSeconds<=0) {
			return;
		}
		for (ValueHolder vh: values.values()) {
			synchronized(vh) {
				ZonedDateTime now=ZonedDateTime.now();
				if (vh.value!=null && vh.time!=null && vh.time.isBefore(now.minusSeconds(expireAfterSeconds))) {
					updateValue(vh, now, null);
				}
			}
		}
	}
	
	@Override
	public void publishMetric(VictronMQTTOutputMetric metric, ZonedDateTime time, String name, Number value, String unit) {
		
		ValueHolder vh=values.get(metric.target);
		if (vh!=null) {
			updateValue(vh, time, value);
		} else {
			log.warn("Unregistered value: {}",metric.target);
		}
	}

	protected void updateValue(ValueHolder vh, ZonedDateTime time, Number value) {
		synchronized(vh) {
			vh.time=time;
			vh.value=value;
			
			StringBuilder json=new StringBuilder();
			StringBuilder jsonClose=new StringBuilder();
			for (String s: vh.target.jsonPath) {
				json.append("{\"").append(s).append("\":");
				jsonClose.append("}");
			}
			json.append((value==null)?"0":value);
			json.append(jsonClose);
			
			mqtt.publish(topic, json.toString().getBytes());
		}
	}

	
}
