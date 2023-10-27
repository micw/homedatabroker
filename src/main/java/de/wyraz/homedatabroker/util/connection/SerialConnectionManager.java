package de.wyraz.homedatabroker.util.connection;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.InputStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Service;

import com.fazecast.jSerialComm.SerialPort;

import de.wyraz.homedatabroker.util.connection.AbstractConnectionManager.IConnection;
import de.wyraz.homedatabroker.util.connection.SerialConnectionManager.SerialConnection;
import de.wyraz.homedatabroker.util.connection.SerialConnectionManager.SerialConnectionParams;
import jakarta.validation.constraints.NotEmpty;

@Service
public class SerialConnectionManager extends AbstractConnectionManager<SerialConnection, SerialConnectionParams> {
	
	@Override
	protected SerialConnection createNewConnection(SerialConnectionParams params) {
		return new SerialConnection(params);
	}
	
	public static class SerialConnectionParams {
		
		@NotEmpty
		protected String port;
		
		protected int baud=9600;
		
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
			return "Serial@"+port;
		}

	}
	
	public interface SerialConnectionConsumer {
		public void consume(InputStream in) throws Exception;
	}
	
	public class SerialConnection implements IConnection {
		
		protected final SerialConnectionParams params;

		protected SerialPort serialPort;
		
		protected SerialConnectionConsumer consumer;
		
		protected Thread consumerThread;
		
		protected SerialConnection(SerialConnectionParams params) {
			this.params=params;
		}
		
		public boolean checkConnection() {
			synchronized(this) {
				
				if (serialPort==null || !serialPort.isOpen()) {

					try {
						SerialPort sp=SerialPort.getCommPort(params.port);
						sp.setBaudRate(params.baud);
						sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
						sp.setNumDataBits(8);
						sp.setParity(SerialPort.NO_PARITY);
						sp.setNumStopBits(1);
						sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100,100);
						if (!sp.openPort()) {
							log.warn("Unable to open {}", params);
							return false;
						}
						this.serialPort = sp;
					} catch (Exception ex) {
						log.warn("Unable to open {}: {}", params, ex.getMessage(), ex);
						return false;
					}
					consume();
				}
				log.info("Connected to {}",params);
				
				return true;
			}
		}
		
		protected void tryClose() {
			synchronized(this) {
				if (serialPort!=null && serialPort.isOpen()) {
					serialPort.closePort();
				}
				serialPort=null;
			}
		}

		protected void consume() {
			synchronized(this) {
				if (this.consumerThread!=null && consumerThread.isAlive()) {
					return;
				}
				if (this.consumer==null || this.serialPort==null || !this.serialPort.isOpen()) {
					return;
				}
				
				final SerialPort port=this.serialPort;
				final SerialConnectionConsumer consumer=this.consumer;
				final InputStream in=new BufferedInputStream(port.getInputStreamWithSuppressedTimeoutExceptions());
				this.consumerThread=new Thread(()->{
					while (port.isOpen()) {
						try {
							consumer.consume(in);
						} catch (EOFException ex) { // serial port was removed
							log.warn("Serial port {} closed.", params);
							tryClose();
							break;
						} catch (Exception ex) {
							// FIXME: handle
							ex.printStackTrace();
						}
						System.err.println(port.isOpen());
					}
					this.consumerThread=null;
				});
				this.consumerThread.start();
			}
		}
		
		public void setConsumer(SerialConnectionConsumer consumer) {
			synchronized(this) {
				if (this.consumer!=null) {
					throw new IllegalStateException("This serial port has already a consumer");
				}
				this.consumer = consumer;
				consume();
			}
		}
		
	}

}
