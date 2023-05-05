>ℹ️&nbsp;&nbsp;SignalFx was acquired by Splunk in October 2019. See [Splunk SignalFx](https://www.splunk.com/en_us/investor-relations/acquisitions/signalfx.html) for more information.

---

<p align="center">
  <a href="https://github.com/signalfx/signalfx-java/actions?query=workflow%3A%22CI+build%22">
    <img alt="Build Status" src="https://github.com/signalfx/signalfx-java/actions/workflows/ci.yaml/badge.svg">
  </a>
</p>

---

# SignalFx client libraries

This repository contains legacy libraries for instrumenting Java applications and
reporting metrics to Splunk Observability Cloud (formerly SignalFx).

Splunk has adopted OpenTelemetry. Use the 
[OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java) or
the [Splunk Distribution of OpenTelemetry Java Instrumentation](https://github.com/signalfx/splunk-otel-java)
agent to send telemetry data to Splunk. Do not integrate `signalfx-java` 
into new services.

To send data to Splunk, you need a Splunk Observability Cloud account and organization
API token. For more information on Observability Cloud and to create an
account, go to [Splunk Observability](https://www.splunk.com/en_us/products/observability.html).

## Language compatibility

* Java 8 and higher

## Using this library in your project

You are discouraged from integrating this library into new services. 
Use [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java) to send data 
to Splunk instead.

Legacy documentation is still here for posterity: [legacy-usage.md](legacy-usage.md). 

# Executing SignalFlow computations

[Learn more about using SignalFlow here](signalflow.md).

## License

Apache Software License v2. Copyright © 2014-2023 SignalFx | Splunk
