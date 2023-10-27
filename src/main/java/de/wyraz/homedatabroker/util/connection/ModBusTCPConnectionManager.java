package de.wyraz.homedatabroker.util.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

@Service
public class ModBusTCPConnectionManager {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	protected static Map<ModBusTCPConnectionParams, ModBusTCPConnection> CONNECTIONS=new HashMap<>();
	
	public ModBusTCPConnection getConnection(ModBusTCPConnectionParams params) {
		synchronized(CONNECTIONS) {
			return CONNECTIONS.computeIfAbsent(params, this::createNewConnection);
		}
	}
	
	protected ModBusTCPConnection createNewConnection(ModBusTCPConnectionParams params) {
		ModBusTCPConnectionImpl connection=new ModBusTCPConnectionImpl(params);
		connection.checkConnection();
		return connection;
	}
	
	protected class ModBusTCPConnectionImpl implements ModBusTCPConnection {
		
		protected final ModBusTCPConnectionParams params;

		protected InetAddress hostAddress;
		protected TCPMasterConnection con;
		
		protected transient int transactionId=1;
		protected transient int errorCounter=0;
		
		protected ModBusTCPConnectionImpl(ModBusTCPConnectionParams params) {
			this.params=params;
		}
		
		public boolean checkConnection() {
			synchronized(this) {
				if (con==null || !con.isConnected()) {
					try {
						hostAddress=InetAddress.getByName(params.host); // resolve on each reconnect (address may have changed)
					} catch (UnknownHostException ex) {
						if (hostAddress==null) {
							log.warn("Unknown host: {}", params.host);
							return false;
						}
						log.warn("Unknown host: {} - using last address {}", params.host, hostAddress);
					}
					
					con=new TCPMasterConnection(hostAddress);
					con.setTimeout(1000);
					con.setPort(params.port);
					
					try {
						con.connect();
					} catch (Exception ex) {
						log.warn("Unable to connect to {}: {}", params, ex.getMessage());
						return false;
					}
					log.info("Connected to {}", params);
					errorCounter=0;
				}
				
				return true;
			}
		}
		
		protected void tryClose() {
			synchronized(this) {
				if (con!=null) {
					try {
						con.close();
					} catch (Exception ex) {
						// ignored
					}
					con=null;
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		public <R extends ModbusResponse> R executeRequest(ModbusRequest request) throws ModbusException {
			synchronized(this) {
				
				if (con==null || !con.isConnected()) {
					throw new ModbusException("Not connected to "+params);
				}
				
				if (request.getTransactionID()==0) {
					request.setTransactionID(++transactionId);
				}
				
				ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
				trans.setRetries(1);
				trans.setReconnecting(false);
				trans.setRequest(request);
				try {
					trans.execute();
					R response=(R)trans.getResponse();
					errorCounter=0;
					return response;
				} catch (ModbusException ex) {
					if (++errorCounter>10) {
						log.warn("Too many ModBus errors - resetting connection");
						tryClose();
						errorCounter=0;
					}
					throw ex;
				}
			}
			
		}
		
	}

}
