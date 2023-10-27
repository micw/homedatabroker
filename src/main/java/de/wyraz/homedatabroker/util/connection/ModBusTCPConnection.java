package de.wyraz.homedatabroker.util.connection;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

public interface ModBusTCPConnection {

	public boolean checkConnection();
	public <R extends ModbusResponse> R executeRequest(ModbusRequest request) throws ModbusException;

}
