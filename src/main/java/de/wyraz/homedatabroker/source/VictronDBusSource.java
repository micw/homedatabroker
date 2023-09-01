package de.wyraz.homedatabroker.source;

import java.util.List;

import org.freedesktop.dbus.annotations.DBusIgnore;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.SimpleDBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class VictronDBusSource extends AbstractScheduledSource {
	
	public static class DBusMetric {
		@NotEmpty
		protected String id;

		@NotNull
		protected String object;
		
		@NotNull
		protected String path;
		
		protected String unit;
	}
	
	@DBusInterfaceName("com.victronenergy.BusItem")
	public static interface IDBusVariant extends DBusInterface {
		public Variant<?> GetValue();
	}
	
	@NotEmpty
	protected String dbusUrl;
	
	@NotEmpty
	protected List<DBusMetric> metrics;
	
	protected DBusConnection dbusCon;

	@PostConstruct
	protected void start() throws Exception {
		dbusCon = new SimpleDBusConnectionBuilder(dbusUrl).build();
		super.start();
	}
	@PreDestroy
	protected void stop() throws Exception {
		if (dbusCon!=null) {
			dbusCon.disconnect();
		}
	}
	
	@Override
	protected void schedule() {
		for (DBusMetric m: metrics) {
			try {
				Variant v=dbusCon.getRemoteObject(m.object, m.path, IDBusVariant.class).GetValue();
				if (v.getValue() instanceof Number) {
					publishMetric(m.id, (Number) v.getValue(), m.unit);
				} else {
					log.warn("DBUS Item on {} {} is not numeric: {}",m.object, m.path,v.getType());
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}

	

}
