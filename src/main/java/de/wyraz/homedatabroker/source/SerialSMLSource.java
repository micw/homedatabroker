package de.wyraz.homedatabroker.source;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.openmuc.jsml.transport.MessageExtractor;
import org.springframework.beans.factory.annotation.Autowired;

import de.wyraz.homedatabroker.util.connection.SerialConnectionManager;
import de.wyraz.homedatabroker.util.connection.SerialConnectionManager.SerialConnection;
import de.wyraz.homedatabroker.util.connection.SerialConnectionManager.SerialConnectionParams;
import de.wyraz.tibberpulse.sml.SMLDecoder;
import de.wyraz.tibberpulse.sml.SMLMeterData;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;

public class SerialSMLSource extends AbstractSource {
	
	@NotNull
	protected SerialConnectionParams connection;
	
	@Autowired
	protected SerialConnectionManager serialManager;

	protected SerialConnection serialCon;
	
	@PostConstruct
	protected void start() {
		serialCon = serialManager.getConnection(connection);
		serialCon.setConsumer((in)->consume(in));
	}
	
	protected void consume(InputStream in) throws Exception {
		DataInputStream dis=new DataInputStream(in);
		for (;;) {
			byte[] sml;
			try {
				sml=new MessageExtractor(dis, 30000).getSmlMessage();
			} catch (EOFException ex) { // Serial port is closed
				throw ex;
			} catch (Exception ex) {
				log.debug("Unable to read SML from serial",ex);
				continue;
			}
				
			SMLMeterData data;
			try {
				data=SMLDecoder.decode(sml, false, true);
			} catch (Exception ex) {
				log.debug("Unable to parse SML",ex);
				continue;
			}
				
			for (SMLMeterData.Reading r: data.getReadings()) {
				publishMetric(StringUtils.firstNonBlank(r.getName(),r.getObisCode()), r.getValue(), r.getUnit());
			}
		}
	}
	
//	
//	
//	
//	public void start() throws Exception {
//		
//		SerialPort sp=SerialPort.getCommPort(port);
//		sp.setBaudRate(baud);
//		sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
//		sp.setNumDataBits(8);
//		sp.setParity(SerialPort.NO_PARITY);
//		sp.setNumStopBits(1);
//		sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100,100);
//		sp.openPort();
//		
//		InputStream in=sp.getInputStreamWithSuppressedTimeoutExceptions();
//		
//		DataInputStream dis=new DataInputStream(new BufferedInputStream(in));
//		
//		for (;;) {
//			try {
//				byte[] sml=new MessageExtractor(dis, 30000).getSmlMessage();
//				System.err.println(LocalTime.now()+" "+sml);
//			} catch (IOException ex) {
//				System.err.println(LocalTime.now()+" "+ex);
//			}
////			
////			int read=in.read(buffer, 0, buffer.length);
////			System.err.println(LocalTime.now()+" "+read+" "+ByteUtil.toHex(buffer,0,read));
////			Thread.sleep(100);
//		}
//	}

}
