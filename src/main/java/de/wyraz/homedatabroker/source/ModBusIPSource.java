package de.wyraz.homedatabroker.source;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;

import de.wyraz.homedatabroker.util.connection.ModBusIPConnectionManager;
import de.wyraz.homedatabroker.util.connection.ModBusIPConnectionManager.AbstractModBusIPConnection;
import de.wyraz.homedatabroker.util.connection.ModBusIPConnectionManager.IpProtocol;
import de.wyraz.homedatabroker.util.connection.ModBusIPConnectionManager.ModBusIPConnectionParams;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * https://csimn.com/MHelp-VP3-TM/vp3-tm-appendix-C.html
 * @author mwyraz
 */
public class ModBusIPSource extends AbstractScheduledSource {
	
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
		int16(2) {
			@Override
			public Number decode(byte[] data, int offset) {
				return
						(data[offset++] << 8 | 
						data[offset++] & 0xFF) << 16 >> 16;
			}
		},
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
	
	@Autowired
	protected ModBusIPConnectionManager modBusManager;

	protected AbstractModBusIPConnection<?> modBus;

	protected final ModBusIPConnectionParams connection;
	
	@NotNull
	protected Integer unitId=0;
	
	@NotEmpty
	protected List<ModBusMetric> metrics;
	
	public ModBusIPSource(IpProtocol protocol) {
		connection = new ModBusIPConnectionParams(protocol);
	}
	
	@PostConstruct
	protected void init() {
		modBus=modBusManager.getConnection(connection);
	}
	
	protected void schedule() {
		
		try {
			synchronized(modBus) {
				if (!modBus.checkConnection()) {
					return;
				}
				for (ModBusMetric metric: metrics) {
					if (metric.type==ModBusRegType.input) {
						
						ReadInputRegistersRequest rreq = new ReadInputRegistersRequest(metric.register,
								(metric.format.byteCount+1)/2); // Registers are in "words" (2 bytes)
						
						rreq.setUnitID(unitId);
	
						log.debug("Request  {} : {}",metric.id, rreq.getHexMessage());
						
						ReadInputRegistersResponse rres = modBus.executeRequest(rreq);
				
						log.debug("Response {}: {}",metric.id, rres.getHexMessage());
						
						Number result=metric.format.decode(rres.getMessage(),1);
						
						if (metric.scale!=null && metric.scale!=0) {
							result = scaleNumber(result, metric.scale);
						}
	
						publishMetric(metric.id, result, metric.unit);
					}
				}
			}
			
			if (lastException!=null) {
				lastException=null; // everything is ok
				log.warn("Modbus errors resolved");
			}
		} catch (Exception ex) {
			if (!checkLastException(ex)) {
				log.warn("Modbus error", ex);
			} else {
				log.info("Modbus error: {}", ex.getMessage());
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
