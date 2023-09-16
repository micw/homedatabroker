# DBus Setup

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
