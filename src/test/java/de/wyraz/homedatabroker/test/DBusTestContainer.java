package de.wyraz.homedatabroker.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class DBusTestContainer extends GenericContainer<DBusTestContainer> {

    public DBusTestContainer() {
        super("ghcr.io/micw/docker-dbus:v1.0");
        withExposedPorts(78);
        withNetwork(Network.SHARED);
    }
    
    @Override
    protected void configure() {
    	super.configure();
    	addFixedExposedPort(7878, 78);
    }
}
