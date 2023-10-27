# ModBus access

* For registers, see https://github.com/victronenergy/dbus_modbustcp/blob/master/CCGX-Modbus-TCP-register-list.xlsx

## Example for reading the battery state via modbus

```
sources:
  - id: battery
    type: modbus-tcp
    connection:
      host: raspberrypi4.local
      port: 502
    unitId: 100
    cron: '*/15 * * * * *'
    metrics:
    - id: power
      type: input
      register: 842
      format: int16
      unit: W
    - id: soc
      type: input
      register: 843
      format: uint16
      unit: '%'

```



# DBus Setup

!!! Do not use DBus over TCP (remote). It must be set up on every update. If a remote DBus connection is interrupted, the DBus device o Venus OS remains in it's last state !!!

- Install VenusOS on Raspberry PI
- [Enable SSH Access](https://www.victronenergy.com/live/ccgx:root_access)
- Enable DBus over TCP
    - Log in into VenusOS
    - Edit /etc/dbus-1/system.conf
    - Add the following lines between the existing `<listen>` block and the `<policy>` block
    - Restart dbus with `/etc/init.d/dbus-1 restart`
- The DBus TCP connection can now be tested from a linux system with the command `dbus-monitor  --address  tcp:host=192.168.2.3,port=78` (replace 192.168.2.3 with the actual IP address of the VenusOS device

## Hints

- A good reference how DBus works can be found [here](https://rm5248.com/d-bus-tutorial/)
- qdbus can be found on archlinux in package "qt5-tools"
