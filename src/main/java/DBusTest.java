import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;

import de.wyraz.homedatabroker.util.vedbus.DBusVariant;

public class DBusTest {
	
	protected static Logger log = LoggerFactory.getLogger(DBusTest.class);
	
	public static void main(String[] args) throws Exception {
		DBusConnection dbusCon=DBusConnectionBuilder.forAddress("tcp:host=raspberrypi4.wyraz.de,port=78")
				.build();
		try {
			run(dbusCon);
		} finally {
//			dbusCon.disconnect();
		}
	}
		
	public static void run(DBusConnection dbusCon) throws Exception {
		
		System.err.println(Arrays.toString(dbusCon.getNames()));
		
		DBus dbus = dbusCon.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
		
		System.err.println(Arrays.toString(dbus.ListNames()));
		
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

		AtomicInteger l1Current=new AtomicInteger(10);
		
		DBusVariant propL1Current=new DBusVariant("/Ac/L1/Current",() -> l1Current.doubleValue());
		DBusVariant propL1Power=new DBusVariant("/Ac/L1/Power",() -> l1Current.doubleValue()*230d);
		
		dbusCon.exportObject(propL1Current);
		dbusCon.exportObject(new DBusVariant("/Ac/L1/Voltage",230d));
		dbusCon.exportObject(propL1Power);
		dbusCon.exportObject(new DBusVariant("/Ac/L2/Current",8.4d));
		dbusCon.exportObject(new DBusVariant("/Ac/L2/Voltage",230d));
		dbusCon.exportObject(new DBusVariant("/Ac/L2/Power",8.4d*230d));
		dbusCon.exportObject(new DBusVariant("/Ac/L3/Current",12.1d));
		dbusCon.exportObject(new DBusVariant("/Ac/L3/Voltage",230d));
		dbusCon.exportObject(new DBusVariant("/Ac/L3/Power",12.1d*230d));

		for (;;) {
			Thread.sleep(1000);
			if (l1Current.incrementAndGet()>11) {
				l1Current.set(1);
			}
			System.err.println("update "+ l1Current.get());
			
			
//			GenericSignal sig = new GenericSignal("/", 6, "Hello there my friends, this is a test of the emergency broadcast system", 22);
			dbusCon.sendMessage(propL1Current.toPropertiesChangedSignal());			
			dbusCon.sendMessage(propL1Power.toPropertiesChangedSignal());			
		}
		
		/*
		dbusCon.exportObject(new DBusString("/Ac/Current","2.3"));
		dbusCon.exportObject(new DBusString("/Ac/Voltage","229.7"));
		dbusCon.exportObject(new DBusString("/Ac/Power",String.valueOf(229.7*2.3)));
		*/
		
//		
//		try {
//			
//		} finally {
//			dbus.releaseBusName("com.victronenergy.grid.test1");
//		}
		
//		System.err.println(dbus.getExportedObject("com.victronenergy.system", "GetValue"));
	}
	

}
