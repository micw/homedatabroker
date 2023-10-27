package de.wyraz.homedatabroker.util.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Service;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import de.wyraz.homedatabroker.util.connection.AbstractConnectionManager.IConnection;
import de.wyraz.homedatabroker.util.connection.ModBusTCPConnectionManager.ModBusTCPConnection;
import de.wyraz.homedatabroker.util.connection.ModBusTCPConnectionManager.ModBusTCPConnectionParams;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Service
public class ModBusTCPConnectionManager extends AbstractConnectionManager<ModBusTCPConnection, ModBusTCPConnectionParams> {
	
	@Override
	protected ModBusTCPConnection createNewConnection(ModBusTCPConnectionParams params) {
		return new ModBusTCPConnection(params);
	}
	
	public static class ModBusTCPConnectionParams {
		
		@NotEmpty
		protected String host;
		
		@NotNull
		protected Integer port=509;
		
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		
		@Override
		public String toString() {
			return "ModBusTCP@"+host+":"+port;
		}

	}
	
	public class ModBusTCPConnection implements IConnection {
		
		protected final ModBusTCPConnectionParams params;

		protected InetAddress hostAddress;
		protected TCPMasterConnection con;
		
		protected transient int transactionId=1;
		protected transient int errorCounter=0;
		
		protected ModBusTCPConnection(ModBusTCPConnectionParams params) {
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
