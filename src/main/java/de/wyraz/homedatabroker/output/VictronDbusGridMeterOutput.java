package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.SimpleDBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.springframework.scheduling.annotation.Scheduled;

import de.wyraz.homedatabroker.util.vedbus.DBusVariant;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class VictronDbusGridMeterOutput extends AbstractOutput<VictronDbusGridMeterOutput.VictronDbusOutputMetric> {

	public static class VictronDbusOutputMetric extends AbstractOutputMetric {
		
		@NotNull
		protected GridValue target;
	}
	
	protected static class ValueHolder {
		protected Number value;
		protected DBusVariant variant;
	}
	
	public static enum GridValue {
		AC_L1_VOLTAGE("/Ac/L1/Voltage",230d),
		AC_L1_CURRENT("/Ac/L1/Current",null),
		AC_L1_POWER("/Ac/L1/Power",null),
		
		AC_L2_VOLTAGE("/Ac/L2/Voltage",230d),
		AC_L2_CURRENT("/Ac/L2/Current",null),
		AC_L2_POWER("/Ac/L2/Power",null),

		AC_L3_VOLTAGE("/Ac/L3/Voltage",230d),
		AC_L3_CURRENT("/Ac/L3/Current",null),
		AC_L3_POWER("/Ac/L3/Power",null),
		;
		
		protected final String path;
		protected final Number initialValue;
		
		private GridValue(String path, Number initialValue) {
			this.path = path;
			this.initialValue = initialValue;
		}
	}

	@NotEmpty
	protected String dbusUrl;
	
	@NotEmpty
	protected List<VictronDbusOutputMetric> metrics;

	@Override
	protected List<VictronDbusOutputMetric> getMetrics() {
		return metrics;
	}
	
	protected DBusConnection dbusCon;
	
	protected Map<GridValue, ValueHolder> values=new ConcurrentHashMap<>();
	
	@PostConstruct
	protected void start() throws Exception {
		for (GridValue gv: GridValue.values()) {
			ValueHolder vh=new ValueHolder();
			vh.value = gv.initialValue;
			vh.variant = new DBusVariant(gv.path,()->vh.value);
			values.put(gv, vh);
		}
		
		tryConnect();
	}
	
	protected boolean hasConnectionError=false;
	
	@Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
	protected synchronized boolean tryConnect() {
		
		if (dbusCon!=null && dbusCon.isConnected()) {
			hasConnectionError=false;
			return true;
		}
		
		try {
			DBusConnection newDbusCon = new SimpleDBusConnectionBuilder(dbusUrl)
					.withShared(false)
					.build();
			
			newDbusCon.requestBusName("com.victronenergy.grid.dbus_grid_31");
			
			newDbusCon.exportObject(new DBusVariant("/Mgmt/ProcessName","homedatabroker"));
			newDbusCon.exportObject(new DBusVariant("/Mgmt/ProcessVersion","1.0.0"));
			newDbusCon.exportObject(new DBusVariant("/Mgmt/Connection","DBus Grid"));
			newDbusCon.exportObject(new DBusVariant("/DeviceInstance",31));
			newDbusCon.exportObject(new DBusVariant("/ProductId","65535"));
			newDbusCon.exportObject(new DBusVariant("/ProductName","DBus Grid"));
			newDbusCon.exportObject(new DBusVariant("/FirmwareVersion","1.0.0"));
			newDbusCon.exportObject(new DBusVariant("/HardwareVersion","1.0.0"));
			newDbusCon.exportObject(new DBusVariant("/CustomName","DBus Grid"));
			newDbusCon.exportObject(new DBusVariant("/Connected","1"));
			newDbusCon.exportObject(new DBusVariant("/Latency",null));
	
			for (ValueHolder vh: values.values()) {
				newDbusCon.exportObject(vh.variant);
			}
			
			this.dbusCon = newDbusCon;
			
			log.info("Connected connect to {}",dbusUrl);
			
			hasConnectionError=false;
			return true;
		} catch (DBusException ex) {
			// reduce log level after the first failure
			if (hasConnectionError) {
				log.trace("Unable to connect to {}",dbusUrl, ex);
			} else {
				log.warn("Unable to connect to {}",dbusUrl, ex);
			}
			
			hasConnectionError=true;
			return false;
		}
	}
	
	@PreDestroy
	protected void stop() throws Exception {
		if (dbusCon!=null) {
			dbusCon.disconnect();
		}
	}
	
	@Override
	public void publishMetric(VictronDbusOutputMetric metric, ZonedDateTime time, String name, Number value,
			String unit) {
		
		if (dbusCon!=null && dbusCon.isConnected()) {
			log.trace("No connection to {}. Dropping value {} = {})",metric.target, value);
			return;
		}
		
		ValueHolder vh=values.get(metric.target);
		if (vh!=null) {
			vh.value=value;
			try {
				dbusCon.sendMessage(vh.variant.toPropertiesChangedSignal());
			} catch (DBusException ex) {
				log.warn("Unable to send PropertiesChangedSignal",ex);
			}
		} else {
			log.warn("Unregistered value: {}",metric.target);
		}
	}
	
}
