package de.wyraz.homedatabroker.util.vedbus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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
	protected final Function<Object,String> toStringFunction;

	public DBusVariant(String path, Supplier<Object> value, Function<Object,String> toStringFunction) {
		this.path = path;
		this.value = value;
		this.toStringFunction = toStringFunction!=null?toStringFunction:(v) -> {
			return v==null?null:v.toString();
		};
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DBusVariant(String path, Object value, Function<Object,String> toStringFunction) {
		this(path, (value instanceof Supplier) ? ((Supplier) value) : () -> value, toStringFunction);
	}
	public DBusVariant(String path, Object value) {
		this(path, value, null);
	}

	@Override
	public String getObjectPath() {
		return path;
	}
	
	public Variant<?> GetValue() {
		Object value=this.value.get();
		if (value==null) {
			return new Variant<>(Float.NaN);
		}
		return new Variant<>(value);
	}
	
	public String GetText() {
		Object value=this.value.get();
		return toStringFunction.apply(value);
	}

	@DBusIgnore
	public PropertiesChanged toPropertiesChangedSignal() throws DBusException {
		Map<String,Variant<?>> changes=new HashMap<>();
		Object value=this.value.get();
		
		if (value instanceof BigDecimal) {
			value=((BigDecimal)value).longValue();
		}
		
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
