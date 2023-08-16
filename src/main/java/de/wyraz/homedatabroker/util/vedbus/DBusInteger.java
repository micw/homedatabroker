package de.wyraz.homedatabroker.util.vedbus;

import java.util.function.Supplier;

import org.freedesktop.dbus.interfaces.DBusInterface;

public class DBusInteger implements DBusInterface {
	
	protected final String path;
	protected final Supplier<Integer> value;
	
	public DBusInteger(String path, Integer value) {
		super();
		this.path = path;
		this.value = () -> value;
	}
	public DBusInteger(String path, Supplier<Integer> value) {
		super();
		this.path = path;
		this.value = value;
	}

	@Override
	public String getObjectPath() {
		return path;
	}
	
	public Integer GetValue() {
		return value.get();
	}
	public String GetText() {
		System.err.println("getText "+path+" "+value);
		return value.get()==null?null:String.valueOf(value.get());
	}

}
