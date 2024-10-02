# HomeDataBroker

This is a middleware that will be able to read metrics from various sources, transform/aggregate it and write it back to various targets.

This project is in an early stage. My plan is to consolidate several of my similar projects into this one.

## Sources

### ModBus-TCP

This source can read ModBus registers via TCP (Modbus RTU over TCP). I use it already in production to read my Growatt TL3-X PV inverter via a PE11 RS485-to-Ethernet adapter.

No documentation available yet, see config.example.yaml for an example that works for my PV inverter.

### MQTT Source (planned)

This source will be able to read arbitrary metrics from MQTT topics. I also plan to integrate my SML parser from https://github.com/micw/tibber-pulse-reader so that SML encoded meter data can directly converted into metrics.

Then it's finished, I can replace https://github.com/micw/mqtt2openmetrics with HomeDataBroker.

### Tibber Pulse HTTP

This source allows to read SML directly from a Tibber Pulse device. It allows to replace https://github.com/micw/tibber-pulse-reader with HomeDataBroker.

## Aggregation

The aggregation source can run calculations on other sources. It has two modes of operation:

### externally triggered aggregations

In this mode, each new input value results in a new output value. Optionally the output can be delayed, so that several inputs that updates the values within a timeframe are aggregates into one output.

TODO: this is not implemented yet!

### cron triggered aggregations

In this mode, all input values are collected. Every defined interval, the aggregation is evaluated and a new output value is published.


## Outputs

### OpenMetrics Http Output

Sends metrics in OpenMetrics format to a HTTP endpoint (e.g. for Victoria Metrics, see  https://docs.victoriametrics.com/#how-to-import-data-in-prometheus-exposition-format).

Supports HTTP(s) and optional basic authentication.

No documentation available yet, see config.example.yaml for an example that works for my PV inverter.

#### Persistence (planned)

When this feature is in place, OpenMetrics Http Output will be able to persist metrics if the endpoint is not available and batch-upload the outstanding metrics as soon as the endpoint is available again

### MQTT Output (work-in-progress)

This output will be able publish metrics via MQTT. MQTT works. Need to implement reconnenct handling, tests, docs.

### Victron Output

This output can be used to feed grid meter data into Victron Venus OS device. My personal use case is to provide grid data to my Victron Multiplus II battery inverter.


## Web UI

Metrics from all sources are exposed via HTTP on port 8080. To disable the webserver, set `spring.main.web-application-type=NONE` in application.yaml or via environment variable.

The following endpoints are available:

* `/healthcheck` - A simple endpoint that just returns 'OK' when the application runs
* `/rest/metrics/sources` - All metrics as REST endpoint (JSON)
* `/metrics/sources` - All metrics as OpenMetrics endpoint to be scraped by Prometheus

A minimal website to display the metrics from `/rest/metrics/sources` is available at `/`.
