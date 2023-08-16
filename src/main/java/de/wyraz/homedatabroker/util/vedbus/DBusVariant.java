package de.wyraz.homedatabroker.util.vedbus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.freedesktop.dbus.annotations.DBusIgnore;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("com.victronenergy.BusItem")
public class DBusVariant implements DBusInterface {
	
	protected final String path;
	protected final Supplier<Object> value;

	public DBusVariant(String path, Supplier<Object> value) {
		this.path = path;
		this.value = value;
	}
	
	public DBusVariant(String path, Object value) {
		this.path = path;
		this.value = (value instanceof Supplier) ? ((Supplier) value) : () -> value;
	}

	@Override
	public String getObjectPath() {
		return path;
	}
	
	public Variant<?> GetValue() {
		Object value=this.value.get();
		System.err.println("getValue "+path+" "+value);
		return new Variant<>(value);
	}
	
	public String GetText() {
		Object value=this.value.get();
		System.err.println("getText "+path+" "+value);
		return value==null?null:value.toString();
	}

	@DBusIgnore
	public PropertiesChanged toPropertiesChangedSignal() throws DBusException {
		Map<String,Variant<?>> changes=new HashMap<>();
		Object value=this.value.get();
		changes.put("Value", new Variant<>(value));
		changes.put("Text", new Variant<>(value==null?null:value.toString()));
		return new PropertiesChanged(path, changes);
	}
	
    class PropertiesChanged extends DBusSignal {
        public PropertiesChanged(String path, Map<String, Variant<?>> changes) throws DBusException {
            super(path, changes);
        }
    }	
}
