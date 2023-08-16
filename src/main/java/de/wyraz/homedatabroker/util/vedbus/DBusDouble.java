package de.wyraz.homedatabroker.util.vedbus;

import java.util.function.Supplier;

import org.freedesktop.dbus.interfaces.DBusInterface;

public class DBusDouble implements DBusInterface {
	
	protected final String path;
	protected final Supplier<Double> value;
	
	public DBusDouble(String path, Double value) {
		super();
		this.path = path;
		this.value = () -> value;
	}
	public DBusDouble(String path, Supplier<Double> value) {
		super();
		this.path = path;
		this.value = value;
	}

	@Override
	public String getObjectPath() {
		return path;
	}
	
	public Double GetValue() {
		System.err.println("getValue "+path+" "+value.get());
		return value.get();
	}
	public String GetText() {
		System.err.println("getText "+path+" "+value.get());
		return value.get()==null?null:String.valueOf(value.get());
	}

}
