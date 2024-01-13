package de.wyraz.homedatabroker.util.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Service;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.io.ModbusUDPTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.net.UDPMasterConnection;

import de.wyraz.homedatabroker.util.connection.AbstractConnectionManager.IConnection;
import de.wyraz.homedatabroker.util.connection.ModBusIPConnectionManager.AbstractModBusIPConnection;
import de.wyraz.homedatabroker.util.connection.ModBusIPConnectionManager.ModBusIPConnectionParams;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Service
public class ModBusIPConnectionManager extends AbstractConnectionManager<AbstractModBusIPConnection<?>, ModBusIPConnectionParams> {
	
	@Override
	protected AbstractModBusIPConnection<?> createNewConnection(ModBusIPConnectionParams params) {
		if (params.protocol==IpProtocol.UDP) {
			return new ModBusUDPConnection(params);
		} else {
			return new ModBusTCPConnection(params);
		}
	}
	
	public static enum IpProtocol {
		TCP,
		UDP,
	}
	
	public static class ModBusIPConnectionParams {
		
		public ModBusIPConnectionParams(IpProtocol protocol) {
			this.protocol = protocol;
		}
		
		protected final IpProtocol protocol;
		
		@NotEmpty
		protected String host;
		
		@NotNull
		protected Integer port=509;
		
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		
		public String getHost() {
			return host;
		}
		
		public Integer getPort() {
			return port;
		}
		
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		
		@Override
		public String toString() {
			return "ModBus"+protocol+"@"+host+":"+port;
		}

	}

	public class ModBusTCPConnection extends AbstractModBusIPConnection<TCPMasterConnection> {

		public ModBusTCPConnection(ModBusIPConnectionParams params) {
			super(params);
		}
		
		@Override
		protected boolean isConnected(TCPMasterConnection con) {
			return con!=null && con.isConnected();
		}
		
		@Override
		protected TCPMasterConnection connect(InetAddress hostAddress) throws Exception {
			TCPMasterConnection con=new TCPMasterConnection(hostAddress);
			con.setTimeout(3000);
			con.setPort(params.port);
			con.connect();
			return con;
		}
		
		@Override
		protected void closeConnection(TCPMasterConnection con) throws IOException {
			con.close();
		}
		
		@Override
		protected ModbusTransaction createTransaction(TCPMasterConnection con) {
			ModbusTCPTransaction tx=new ModbusTCPTransaction(con);
			tx.setRetries(0);
			tx.setReconnecting(false);
			return tx;
		}
		
	}

	public class ModBusUDPConnection extends AbstractModBusIPConnection<UDPMasterConnection> {

		public ModBusUDPConnection(ModBusIPConnectionParams params) {
			super(params);
		}
		
		@Override
		protected boolean isConnected(UDPMasterConnection con) {
			return con!=null && con.isConnected();
		}
		
		@Override
		protected UDPMasterConnection connect(InetAddress hostAddress) throws Exception {
			UDPMasterConnection con=new UDPMasterConnection(hostAddress);
			con.setTimeout(3000);
			con.setPort(params.port);
			con.connect();
			return con;
		}
		
		@Override
		protected void closeConnection(UDPMasterConnection con) throws IOException {
			con.close();
		}
		
		@Override
		protected ModbusTransaction createTransaction(UDPMasterConnection con) {
			ModbusUDPTransaction tx=new ModbusUDPTransaction(con);
			tx.setRetries(0);
			return tx;
		}
		
	}
	
	public abstract class AbstractModBusIPConnection<TModbus> implements IConnection {
		
		protected final ModBusIPConnectionParams params;

		protected InetAddress hostAddress;
		protected TModbus connection;
		
		protected transient int transactionId=1;
		protected transient int errorCounter=0;
		
		protected AbstractModBusIPConnection(ModBusIPConnectionParams params) {
			this.params=params;
		}
		
		protected abstract boolean isConnected(TModbus con);
		protected abstract TModbus connect(InetAddress hostAddress) throws Exception;
		protected abstract void closeConnection(TModbus con) throws Exception;
		protected abstract ModbusTransaction createTransaction(TModbus con);
		
		public boolean checkConnection() {
			synchronized(this) {
				if (!isConnected(connection)) {
					try {
						hostAddress=InetAddress.getByName(params.host); // resolve on each reconnect (address may have changed)
					} catch (UnknownHostException ex) {
						if (hostAddress==null) {
							log.warn("Unknown host: {}", params.host);
							return false;
						}
						log.warn("Unknown host: {} - using last address {}", params.host, hostAddress);
					}
					
					
					try {
						connection=connect(hostAddress);
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
				if (connection!=null) {
					try {
						closeConnection(connection);
					} catch (Exception ex) {
						// ignored
					}
					connection=null;
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		public <R extends ModbusResponse> R executeRequest(ModbusRequest request) throws ModbusException {
			synchronized(this) {
				
				if (!isConnected(connection)) {
					throw new ModbusException("Not connected to "+params);
				}
				
				if (request.getTransactionID()==0) {
					request.setTransactionID(++transactionId);
				}
				
				ModbusTransaction trans = createTransaction(connection);
				trans.setRequest(request);
				try {
					trans.execute();
					R response=(R)trans.getResponse();
					if (response==null) {
						throw new ModbusException("Response was NULL");
					}
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
