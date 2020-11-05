[![Example Code header](https://github.com/newrelic/opensource-website/raw/master/src/images/categories/Example_Code.png)](https://opensource.newrelic.com/oss-category/#example-code)

# opentelemetry-jaeger-exporter-java

As with any technology, in order to get to grips with OpenTelemetry there is a small amount of terminology that it is useful to know:

* Trace: a record of activity for a request through a distributed system. A trace is a [Directed Acyclic Graph](https://en.wikipedia.org/wiki/Directed_acyclic_graph%5C) of spans.
* Spans: named, timed operations representing a single operation within a trace. Spans can be nested to form a trace tree. Each trace contains a root span, which typically describes the end-to-end latency and (optionally) one or more sub-spans for its sub-operations.
* Metrics: a raw measurement about a service, captured at runtime. OpenTelemetry defines three metric instruments — counter, measure and observer. An observer supports an asynchronous API collecting metric data on-demand, once per collection interval.
* Context: a span contains a span context, which is a set of globally unique identifiers that represent the unique request that each span is a part of, representing the data required for moving trace information across service boundaries. OpenTelemetry also supports the correlation context which can carry any user-defined properties. Correlation context is not required and components may choose not to carry or store this information.
* Context Propagation: the means by which context is bundled and transferred between services, typically via HTTP headers. Context propagation is a key part of the OpenTelemetry system, and has some interesting use cases beyond tracing — for example when doing A/B testing. Note that OpenTelemetry supports multiple protocols for context propagation and to avoid issues, it is important that you use a single method throughout your application. So for example, if you use the W3C specification in one service, you need to use it everywhere in your system. These are the currently supported options:
  * SUPER IMPORTANT! [W3C Trace-Context HTTP Propagator](https://w3c.github.io/trace-context/)
  * [W3C Correlation-Context HTTP Propagator](https://w3c.github.io/correlation-context/)
  * [B3 Zipkin HTTP Propagator](https://github.com/openzipkin/b3-propagation)


![](https://github.com/andrew-lozoya/opentelemetry-jeager-exporter-java/blob/main/Resources/2020-10-10_13-36-25.png)
