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
	
	boolean shared = true;
	boolean registerSelf = true;

	public SimpleDBusConnectionBuilder withShared(boolean shared) {
		this.shared = shared;
		return this;
	}

	public SimpleDBusConnectionBuilder withRegisterSelf(boolean registerSelf) {
		this.registerSelf = registerSelf;
		return this;
	}
	
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

        DBusConnection c;
        if (shared) {
            synchronized (DBusConnection.CONNECTIONS) {
                String busAddressStr = transportCfg.getBusAddress().toString();
                c = DBusConnection.CONNECTIONS.get(busAddressStr);
                if (c != null) {
                    c.concurrentConnections.incrementAndGet();
                    return c; // this connection already exists, do not change anything
                } else {
                    c = new DBusConnection(shared, machineId, transportCfg, cfg);
                    DBusConnection.CONNECTIONS.put(busAddressStr, c);
                }
            }
        } else {
            c = new DBusConnection(shared, machineId, transportCfg, cfg);
        }

        c.setDisconnectCallback(getDisconnectCallback());
        c.setWeakReferences(isWeakReference());
        DBusConnection.setEndianness(getEndianess());
        c.connect(registerSelf);
        return c;
    }	
}
