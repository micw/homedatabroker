package de.wyraz.homedatabroker.util.vedbus;

import org.freedesktop.dbus.interfaces.DBusInterface;

public class DBusString implements DBusInterface {
	
	protected final String path;
	protected final String value;
	
	public DBusString(String path, String value) {
		super();
		this.path = path;
		this.value = value;
	}

	@Override
	public String getObjectPath() {
		return path;
	}
	
	public String GetValue() {
		System.err.println("getValue "+path+" "+value);
		return value;
	}
	public String GetText() {
		System.err.println("getText "+path+" "+value);
		return value==null?null:value.toString();
	}

}
