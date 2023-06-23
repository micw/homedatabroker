# HomeDataBroker

This is a middleware that will be able to read metrics from various sources, transform/aggregate it and write it back to various targets.

This project is in an early stage. My plan is to consolidate several of my similar projects into this one.

## Sources

### ModBus-TCP

This source can read ModBus registers via TCP (Modbus RTU over TCP). I use it already in production to read my Growatt TL3-X PV inverter via a PE11 RS485-to-Ethernet adapter.

No documentation available yet, see config.example.yaml for an example that works for my PV inverter.

### MQTT (planned)

This source will be able to read arbitrary metrics from MQTT topics. I also plan to integrate my SML parser from https://github.com/micw/tibber-pulse-reader so that SML encoded meter data can directly converted into metrics.

Then it's finished, I can replace https://github.com/micw/mqtt2openmetrics with HomeDataBroker.

### Tibber Pulse HTTP (planned)

This source will allow to read SML directly from a Tibber Pulse device. It will allow to replace https://github.com/micw/tibber-pulse-reader with HomeDataBroker.

## Outputs

### OpenMetrics Http Output

Sends metrics in OpenMetrics format to a HTTP endpoint (e.g. for Victoria Metrics, see  https://docs.victoriametrics.com/#how-to-import-data-in-prometheus-exposition-format).

Supports HTTP(s) and optional basic authentication.

No documentation available yet, see config.example.yaml for an example that works for my PV inverter.