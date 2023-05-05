
# Executing SignalFlow computations

SignalFlow is SignalFx's real-time analytics computation language. The
SignalFlow API allows SignalFx users to execute real-time streaming analytics
computations on the SignalFx platform. For more information, head over to our
Developers documentation:

* [SignalFlow Overview](https://developers.signalfx.com/signalflow_analytics/signalflow_overview.html)
* [SignalFlow API Reference](https://developers.signalfx.com/signalflow_reference.html)

Executing a SignalFlow program is very simple with this client library:

```java
String program = "data('cpu.utilization').mean().publish()";
SignalFlowClient flow = new SignalFlowClient("MY_TOKEN");
System.out.println("Executing " + program);
Computation computation = flow.execute(program);
for (ChannelMessage message : computation) {
    switch (message.getType()) {
    case DATA_MESSAGE:
        DataMessage dataMessage = (DataMessage) message;
        System.out.printf("%d: %s%n",
                dataMessage.getLogicalTimestampMs(), dataMessage.getData());
        break;

    case EVENT_MESSAGE:
        EventMessage eventMessage = (EventMessage) message;
        System.out.printf("%d: %s%n",
                eventMessage.getTimestampMs(),
                eventMessage.getProperties());
        break;
    }
}
```

Metadata about the timeseries is received from the iterable stream, and it
is also automatically intercepted by the client library and made available through
the ``Computation`` object returned by ``execute()``:

```java
case DATA_MESSAGE:
    DataMessage dataMessage = (DataMessage) message;
    for (Map<String, Number> datum : dataMessage.getData()) {
        Map<String,Object> metadata = computation.getMetadata(datum.getKey());
        // ...
    }
```