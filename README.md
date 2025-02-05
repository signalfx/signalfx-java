>ℹ️&nbsp;&nbsp;SignalFx was acquired by Splunk in October 2019. See [Splunk SignalFx](https://www.splunk.com/en_us/investor-relations/acquisitions/signalfx.html) for more information.

# SignalFx client libraries

* :warning: This library has reached end of support.

This library is now in permanent public archive and will no longer receive updates..

Splunk has adopted OpenTelemetry. Use the 
[OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java) or
the [Splunk Distribution of OpenTelemetry Java Instrumentation](https://github.com/signalfx/splunk-otel-java)
agent to send telemetry data to Splunk. Do not integrate `signalfx-java` 
into new services.

## Do not use this library in your project

You should not use this library in new services. 
Use [OpenTelemetry](https://github.com/open-telemetry/opentelemetry-java) to send data to Splunk instead.

To send data to Splunk, you need a Splunk Observability Cloud account and organization
API token. For more information on Observability Cloud and to create an
account, go to [Splunk Observability](https://www.splunk.com/en_us/products/observability.html).

Legacy documentation is still here for posterity: [legacy-usage.md](legacy-usage.md). 

## License

Apache Software License v2. Copyright © 2014-2023 SignalFx | Splunk
