package com.example;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;

import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpansProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.Charset;

public class BasicHttpURLConnectionClient {

    private static final Tracer tracer = OpenTelemetry.getTracerProvider().get("OpenTelemetrySDK", "3.0");

    // Inject the span context into the request
    private static final HttpTextFormat.Setter<HttpURLConnection> setter =
            new HttpTextFormat.Setter<HttpURLConnection>() {
                @Override
                public void set(HttpURLConnection carrier, String key, String value) {
                    // Insert the context as Header
                    carrier.setRequestProperty(key, value);
                }
            };

    private static void setupExporters() {
        // Jaeger Endpoint URL and PORT
        String ip = "localhost";
        int port = 14250;

        // Create a channel towards Jaeger end point
        ManagedChannel jaegerChannel =
                ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();

        // Build the Jaeger exporter
        SpanExporter exporter =
                JaegerGrpcSpanExporter.newBuilder()
                        .setServiceName("OpenTelemetrySDK.BasicHttpURLConnectionClient")
                        .setChannel(jaegerChannel)
                        .setDeadlineMs(30000)
                        .build();

        // Register the Jaeger exporter with the span processor on immediately forwards ended spans to the LoggingSpan exporter
        OpenTelemetrySdk.getTracerProvider().addSpanProcessor(SimpleSpansProcessor.newBuilder(new LoggingSpanExporter()).build());

        // Register the Jeager exporter with the span processor on our tracer to bulk send ended spans
        // OpenTelemetrySdk.getTracerProvider().addSpanProcessor(BatchSpansProcessor.newBuilder(exporter).build());

        // Register the Jeager exporter with the span processor on our tracer to immediately forwards ended spans to the
        OpenTelemetrySdk.getTracerProvider().addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());

    }
    private static void HttpClient() throws Exception {

        int status = 0;
        StringBuilder content = new StringBuilder();

        // Tomcat Server variables with a New Relic Agent
        int port = 8080;
        URL url = new URL("http://localhost:" + port);

        // Create an OpenTelemetry `Tracer`
        // Name convention for the Span is not yet defined.
        Span span = tracer.spanBuilder("/demo/ResponseServlet").setSpanKind(Span.Kind.CLIENT).startSpan();
        try (Scope parentScope = tracer.withSpan(span)) {
            span.setAttribute("component", "http");
            span.setAttribute("http.method", "GET");
            /*
              Only one of the following is required:
                - http.url
                - http.scheme, http.host, http.target
                - http.scheme, peer.hostname, peer.port, http.target
                - http.scheme, peer.ip, peer.port, http.target
             */
            span.setAttribute("http.url", url.toString());

            //Do work

            // Use for Proxyman to observe HTTP/HTTPS requests locally
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 9090));
            HttpURLConnection transportLayer = (HttpURLConnection) url.openConnection(); //(proxy);

            // Inject the request with the *current*  Context, which contains our current Span.
            OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), transportLayer, setter);

            try {
                // Connect to sever
                transportLayer.setRequestMethod("GET");
                status = transportLayer.getResponseCode();

                // Process the request from the BufferReader
                doSomeWork(transportLayer, content);

                span.setStatus(Status.OK);
            } catch (Exception e) {
                span.setStatus(Status.UNKNOWN.withDescription("HTTP Code: " + status));
            }
        } finally {

            System.out.println();
            // Output the result of the request
            System.out.println("Response Code: " + status);
            System.out.println("Response Msg: " + content);
            System.out.println();
            // Close the Span
            System.out.println("Parent Span:");
            span.end();
        }
    }

    private static void doSomeWork(HttpURLConnection transportLayer, StringBuilder content) throws InterruptedException {
        Span span = tracer.spanBuilder("BufferReader")
                // NOTE: setParent(parentSpan) is  not required anymore
                // `tracer.getCurrentSpan()` is automatically added as parent
                .startSpan();
        try (Scope ignored = tracer.withSpan(span)) {
            //Do child work
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(transportLayer.getInputStream(), Charset.defaultCharset()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            span.setStatus(Status.OK);
        } catch (Exception e) {
            span.setStatus(Status.UNKNOWN.withDescription("UNKNOWN"));
        } finally {
            System.out.println();
            System.out.println("Child Span:");
            // Close the Span
            span.end();
            Thread.sleep(100);
        }
    }

    public static void main(String[] args) {
        setupExporters();

        // Perform request every 10s
        Thread loop =
                new Thread(() -> {
                    while (true) {
                        try {
                            HttpClient();
                            Thread.sleep(10000);
                            System.out.println();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                });
        loop.start();
    }
}