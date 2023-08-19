package de.wyraz.homedatabroker.source;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * https://csimn.com/MHelp-VP3-TM/vp3-tm-appendix-C.html
 * @author mwyraz
 */
public class ModBusTCPSource extends AbstractSource {
	
	public static class ModBusMetric {
		@NotEmpty
		protected String id;
		
		@NotNull
		protected ModBusRegType type;
		
		@NotNull
		protected Integer register;
		
		@NotNull
		protected ModBusRegFormat format;
		
		protected Integer scale;
		
		protected String unit;
	}

	public static enum ModBusRegType {
		input,
		;
	}
	
	public static enum ModBusRegFormat {
		uint16(2) {
			@Override
			public Number decode(byte[] data, int offset) {
				return
						(data[offset++] & 0xFF) << 8 | 
						data[offset++] & 0xFF;
			}
		},
		uint32(4) {
			@Override
			public Number decode(byte[] data, int offset) {
				return
						((long) data[offset++] & 0xFF) << 24 | 
						(data[offset++] & 0xFF) << 16 | 
						(data[offset++] & 0xFF) << 8 | 
						data[offset++] & 0xFF;
			}
		},
		;
		final int byteCount;
		
		private ModBusRegFormat(int byteCount) {
			this.byteCount=byteCount;
		}
		
		public abstract Number decode(byte[] data, int offset);
	}
	
	
	@NotEmpty
	protected String host;
	
	@NotNull
	protected Integer port=509;
	
	@NotNull
	protected Integer unitId=0;
	
	@NotEmpty
	protected List<ModBusMetric> metrics;

	@NotEmpty
	protected String cron;
	
	@Autowired
	protected TaskScheduler scheduler;

	protected static Map<String,Object> connectionLocks=new ConcurrentHashMap<>();
	
	@PostConstruct
	protected void start() {
		scheduler.schedule(() -> { schedule(); }, new CronTrigger(cron));
	}
	
	protected InetAddress hostAddress;
	protected TCPMasterConnection con;
	
	protected void schedule() {
		
		Object lock=connectionLocks.computeIfAbsent(host+":"+port, (k) -> new Object());
		
		synchronized(lock) {
			if (con==null || !con.isConnected()) {
				try {
					hostAddress=InetAddress.getByName(host); // resolve on each reconnect (address may have changed)
				} catch (UnknownHostException ex) {
					if (!checkLastException(ex)) {
						if (hostAddress==null) {
							log.warn("Unknown host: {}", host);
							return;
						}
						log.warn("Unknown host: {} - using last address {}", host, hostAddress);
					}
				}
				
				con=new TCPMasterConnection(hostAddress);
				con.setTimeout(1000);
				con.setPort(8899);
			}
			
			try {
				con.connect();
			} catch (Exception ex) {
				if (!checkLastException(ex)) {
					log.warn("Unable to connect to {}:{}: {}", host, port, ex.getMessage());
				}
				return;
			}
	
			try {
				
				for (ModBusMetric metric: metrics) {
					if (metric.type==ModBusRegType.input) {
						
						ReadInputRegistersRequest rreq = new ReadInputRegistersRequest(metric.register,
								(metric.format.byteCount+1)/2); // Registers are in "words" (2 bytes)
						
						rreq.setUnitID(unitId);
						
						ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
						trans.setRetries(1);
						trans.setReconnecting(false);
						trans.setRequest(rreq);
						trans.execute();
	
						log.debug("Request  {} : {}",metric.id, rreq.getHexMessage());
	
						ReadInputRegistersResponse rres = (ReadInputRegistersResponse) trans.getResponse();
				
						log.debug("Response {}: {}",metric.id, rres.getHexMessage());
						
						Number result=metric.format.decode(rres.getMessage(),1);
						
						if (metric.scale!=null && metric.scale!=0) {
							result = scaleNumber(result, metric.scale);
						}
	
						publishMetric(metric.id, result, metric.unit);
					}
				}
				
				if (lastException!=null) {
					lastException=null; // everything is ok
					log.warn("Modbus errors resolved");
				}
			} catch (Exception ex) {
				if (!checkLastException(ex)) {
					log.warn("Modbus error", ex);
				}
			}
		}
	}
	

	// FIXME: copied from my SML decode - de-dupplicate later
	public Number scaleNumber(Number value, Number scaler) {
		if (value==null) {
			return null;
		}
		
		int sc=(scaler==null)?0:scaler.intValue();

		if (value instanceof Byte) {
			byte val=(Byte)value;
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (value instanceof Short) {
			short val=(Short)value;
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}
		
		if (value instanceof Integer) {
			int val=(Integer) value;
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}

		if (value instanceof Long) {
			long val=(Long) value;
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}
		
		if (value instanceof BigInteger) {
			BigInteger val=(BigInteger) value;
			if (sc==0) {
				return val;
			}
			return new BigDecimal(val).scaleByPowerOfTen(sc);
		}
		
		
		log.warn("Number conversion not implemented: {}",value.getClass().getName());
		return null;
	}
	
	
}
