package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.utils.Util;

public class SimpleDBusConnectionBuilder extends BaseConnectionBuilder<SimpleDBusConnectionBuilder, DBusConnection> {
	
	public SimpleDBusConnectionBuilder(BusAddress _address) {
		super(SimpleDBusConnectionBuilder.class, _address);
	}
	public SimpleDBusConnectionBuilder(String _address) {
		this(BusAddress.of(_address));
	}

	final String machineId=String.format("%s@%s", Util.getCurrentUser(), Util.getHostName());;

    /**
     * Create the new {@link DBusConnection}.
     *
     * @return {@link DBusConnection}
     * @throws DBusException when DBusConnection could not be opened
     */
    @Override
    public DBusConnection build() throws DBusException {
        ReceivingServiceConfig cfg = buildThreadConfig();
        TransportConfig transportCfg = buildTransportConfig();

        DBusConnection c = new DBusConnection(false, machineId, transportCfg, cfg);

        c.setDisconnectCallback(getDisconnectCallback());
        c.setWeakReferences(isWeakReference());
        DBusConnection.setEndianness(getEndianess());
        c.connect(true);
        return c;
    }	
}
