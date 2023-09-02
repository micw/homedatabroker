package de.wyraz.homedatabroker.test;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class MosquittoTestContainer extends GenericContainer<MosquittoTestContainer> {

    public MosquittoTestContainer() {
        super("library/eclipse-mosquitto:2.0.16");
        withExposedPorts(1883);
        withNetwork(Network.SHARED);
        withClasspathResourceMapping("res/mosquitto.conf", "/mosquitto/config/mosquitto.conf", BindMode.READ_ONLY);
    }
    
    @Override
    protected void configure() {
    	super.configure();
    	addFixedExposedPort(18831, 1883);
    }
}
