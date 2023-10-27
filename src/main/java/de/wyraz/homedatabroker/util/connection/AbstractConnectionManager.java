package de.wyraz.homedatabroker.util.connection;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import de.wyraz.homedatabroker.util.connection.AbstractConnectionManager.IConnection;

public abstract class AbstractConnectionManager<TCon extends IConnection, TConParams> {
	protected Logger log = LoggerFactory.getLogger(getClass());

	protected Map<TConParams, TCon> CONNECTIONS=new HashMap<>();
	
	public TCon getConnection(TConParams params) {
		synchronized(CONNECTIONS) {
			return CONNECTIONS.computeIfAbsent(params, (p) -> {
				TCon con=createNewConnection(params);
				con.checkConnection();
				return con;
			});
		}
	}
	
	@Scheduled(cron = "50 * * * * *")
	protected void checkConnections() {
		for (TCon con: CONNECTIONS.values()) {
			con.checkConnection();
		}
	}
	
	protected abstract TCon createNewConnection(TConParams params);
	
	public static interface IConnection {

		public abstract boolean checkConnection();

	}
	
	
	
}
