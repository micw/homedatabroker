package de.wyraz.homedatabroker.source;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.SimpleDBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;
import org.springframework.scheduling.annotation.Scheduled;

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
		
		protected boolean negate;
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
		tryConnect();
		super.start();
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
			
			this.dbusCon = newDbusCon;
			
			log.info("Connected to {}",dbusUrl);
			
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
	protected void schedule() {
		for (DBusMetric m: metrics) {
			try {
				Variant<?> v=dbusCon.getRemoteObject(m.object, m.path, IDBusVariant.class).GetValue();
				if (v.getValue() instanceof Number) {
					Number value=(Number) v.getValue();
					if (m.negate) {
						value=-value.doubleValue();
					}
					
					publishMetric(m.id, value, m.unit);
				} else {
					log.warn("DBUS Item on {} {} is not numeric: {}",m.object, m.path,v.getType());
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}

	

}
