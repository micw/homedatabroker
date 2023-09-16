package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.SimpleDBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.springframework.scheduling.annotation.Scheduled;

import de.wyraz.homedatabroker.util.vedbus.DBusVariant;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * https://github.com/victronenergy/venus/wiki/dbus-api
 * https://github.com/victronenergy/venus/wiki/dbus
 */
public class VictronDbusGridMeterOutput extends AbstractOutput<VictronDbusGridMeterOutput.VictronDbusOutputMetric> {

	public static class VictronDbusOutputMetric extends AbstractOutputMetric {
		
		@NotNull
		protected GridValue target;
	}
	
	protected static class ValueHolder {
		protected Number value;
		protected DBusVariant variant;
	}
	
	protected static final Function<Object, String> STR_WATT=(value) -> {
		if (value==null) return null;
		return String.format(Locale.ENGLISH,"%,.1f W", value);
	};
	protected static final Function<Object, String> STR_VOLT=(value) -> {
		if (value==null) return null;
		return String.format(Locale.ENGLISH,"%,.1f V", value);
	};
	protected static final Function<Object, String> STR_AMPERE=(value) -> {
		if (value==null) return null;
		return String.format(Locale.ENGLISH,"%,.1f A", value);
	};
	
	public static enum GridValue {
		
		// TODO: Energy requires unit conversion (Wh -> kWh)
		//AC_ENERGY_FORWARD("/Ac/Energy/Forward",null),
		//AC_ENERGY_REVERSE("/Ac/Energy/Forward",null),
		
		
		AC_POWER("/Ac/Power",null,STR_WATT),
		
		AC_L1_VOLTAGE("/Ac/L1/Voltage",null,STR_VOLT),
		AC_L1_CURRENT("/Ac/L1/Current",null,STR_AMPERE),
		AC_L1_POWER("/Ac/L1/Power",null,STR_WATT),
		
		AC_L2_VOLTAGE("/Ac/L2/Voltage",null,STR_VOLT),
		AC_L2_CURRENT("/Ac/L2/Current",null,STR_AMPERE),
		AC_L2_POWER("/Ac/L2/Power",null,STR_WATT),

		AC_L3_VOLTAGE("/Ac/L3/Voltage",null,STR_VOLT),
		AC_L3_CURRENT("/Ac/L3/Current",null,STR_AMPERE),
		AC_L3_POWER("/Ac/L3/Power",null,STR_WATT),
		;
		
		protected final String path;
		protected final Number initialValue;
		protected final Function<Object, String> toStringFunction;
		
		private GridValue(String path, Number initialValue, Function<Object, String> toStringFunction) {
			this.path = path;
			this.initialValue = initialValue;
			this.toStringFunction = toStringFunction;
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
			vh.variant = new DBusVariant(gv.path,()->vh.value, gv.toStringFunction);
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
			newDbusCon.exportObject(new DBusVariant("/ProductName","Homedatabroker Grid Meter"));
			newDbusCon.exportObject(new DBusVariant("/FirmwareVersion","1.0.0"));
			newDbusCon.exportObject(new DBusVariant("/HardwareVersion","1.0.0"));
			newDbusCon.exportObject(new DBusVariant("/CustomName","Homedatabroker Grid Meter"));
			newDbusCon.exportObject(new DBusVariant("/Connected",1d));
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
		
		ValueHolder vh=values.get(metric.target);
		if (vh!=null) {
			vh.value=value;
			if (dbusCon!=null && dbusCon.isConnected()) {
				try {
					dbusCon.sendMessage(vh.variant.toPropertiesChangedSignal());
				} catch (DBusException ex) {
					log.warn("Unable to send PropertiesChangedSignal",ex);
				}
			}
		} else {
			log.warn("Unregistered value: {}",metric.target);
		}
	}
	
}
