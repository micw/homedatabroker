# Sources

## MQTT

```
sources:

  - id: mqtt
    type: mqtt
    mqttHost: 192.168.1.1
    mqttUsername: user
    mqttPassword: pass
    subscribe:
      - topics:
         - solar/ac/power
        metric: pv.power
      - topics:
          - solar/+/+/power
          - solar/+/+/yieldtotal
          - solar/+/+/yieldday
          - solar/+/0/frequency
          - solar/+/0/temperature
        metric: pv.$(TOPIC:4).$(TOPIC:2).$(TOPIC:3)
      - topics:
          - gridmeter/sensor/sml/state
        extract: SML_HEX
        metric: gridmeter.$(SML:NAME)
```

The first subscription maps a single topic to a single metric.

The second subscription maps several topics to individual metrics by using parts of the topic as placeholders.

The third subscription decodes the topic's messages as SML (hex encoded) and uses the name of the SML value as placeholder for the metric name. For a list of SML names see [src/main/java/de/wyraz/tibberpulse/sml/ObisNameMap.java](ObisNameMap.java).


