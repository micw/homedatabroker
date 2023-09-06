package de.wyraz.homedatabroker.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.Container.ExecResult;

import de.wyraz.homedatabroker.metric.MetricRegistry;
import de.wyraz.homedatabroker.output.VictronDbusGridMeterOutput.GridValue;
import de.wyraz.homedatabroker.output.VictronDbusGridMeterOutput.VictronDbusOutputMetric;
import de.wyraz.homedatabroker.test.DBusTestContainer;

public class VictronDbusGridMeterOutputIntegrationTests {
	
	@Rule
	public DBusTestContainer dbusServer = new DBusTestContainer();
	
	protected List<String> dbusQuery(String... query) throws Exception {
		String[] cmd= {"qdbus","--bus","tcp:host=127.0.0.1,port=78"};
		for (String s: query) {
			cmd=org.testcontainers.shaded.org.bouncycastle.util.Arrays.append(cmd, s);
		}
		ExecResult res = dbusServer.execInContainer(cmd);
		assertThat(res.getExitCode()).as("Exit code").isEqualTo(0);
		return Arrays.asList(res.getStdout().trim().split("[\r\n]+"));
	}
	
	protected VictronDbusGridMeterOutput veOutput;

	protected MetricRegistry metricRegistry;
	
	@Before
	public void setup() throws Exception {
		
		veOutput=new VictronDbusGridMeterOutput();
		
		veOutput.dbusUrl="tcp:host=127.0.0.1,port=7878";
		veOutput.metrics = new ArrayList<>();

		{
			VictronDbusOutputMetric metric = new VictronDbusOutputMetric();
			metric.source = "test.m1";
			metric.target = GridValue.AC_L1_POWER;
			veOutput.metrics.add(metric);
		}
		veOutput.start();

		metricRegistry = new MetricRegistry();
		veOutput.registry = metricRegistry;
		veOutput.subscribeMetrics();

	}

	@After
	public void teardown() {
		try {
			veOutput.stop();
		} catch (Exception ex) {
			
		}
	}
	
	@Test
	public void testConnectAndDisconnect() throws Exception {
		assertThat(dbusQuery()).contains(" com.victronenergy.grid.dbus_grid_31");
		veOutput.stop();
		assertThat(dbusQuery()).doesNotContain(" com.victronenergy.grid.dbus_grid_31");
	}
	
	@Test
	public void testAutoReconnectDisconnect() throws Exception {
		assertThat(dbusQuery()).contains(" com.victronenergy.grid.dbus_grid_31");
		dbusServer.stop();
		dbusServer.start();
		
		Awaitility.await().atMost(2,TimeUnit.SECONDS).until(()->veOutput.tryConnect());
		
		Awaitility.await().atMost(2,TimeUnit.SECONDS).untilAsserted(() -> {
			assertThat(dbusQuery()).contains(" com.victronenergy.grid.dbus_grid_31");			
		});
	}

}
