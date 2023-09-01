package de.wyraz.homedatabroker.output;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

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
		
		dbusCon=DBusConnectionBuilder.forAddress(dbusUrl).build();
		
		dbusCon.requestBusName("com.victronenergy.grid.dbus_grid_31");
		
		dbusCon.exportObject(new DBusVariant("/Mgmt/ProcessName","homedatabroker"));
		dbusCon.exportObject(new DBusVariant("/Mgmt/ProcessVersion","1.0.0"));
		dbusCon.exportObject(new DBusVariant("/Mgmt/Connection","DBus Grid"));
		dbusCon.exportObject(new DBusVariant("/DeviceInstance",31));
		dbusCon.exportObject(new DBusVariant("/ProductId","65535"));
		dbusCon.exportObject(new DBusVariant("/ProductName","DBus Grid"));
		dbusCon.exportObject(new DBusVariant("/FirmwareVersion","1.0.0"));
		dbusCon.exportObject(new DBusVariant("/HardwareVersion","1.0.0"));
		dbusCon.exportObject(new DBusVariant("/CustomName","DBus Grid"));
		dbusCon.exportObject(new DBusVariant("/Connected","1"));
		dbusCon.exportObject(new DBusVariant("/Latency",null));
		
		for (GridValue gv: GridValue.values()) {
			ValueHolder vh=new ValueHolder();
			vh.value = gv.initialValue;
			vh.variant = new DBusVariant(gv.path,()->vh.value);
			values.put(gv, vh);
			dbusCon.exportObject(vh.variant);
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
