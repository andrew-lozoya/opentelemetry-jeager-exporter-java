package com.example;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class BasicHttpServerExample {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        HttpContext context = server.createContext("/");
        context.setHandler(BasicHttpServerExample::handleRequest);
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        printRequestInfo(exchange);
        String response = "This is the response at " + requestURI;
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void printRequestInfo(HttpExchange exchange) {
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod != null) {
            System.out.println("-- HTTP.METHOD --");
            System.out.println(requestMethod);
        }

        System.out.println("-- HEADERS --");
        Headers requestHeaders = exchange.getRequestHeaders();
        requestHeaders.entrySet().forEach(System.out::println);

        HttpPrincipal principal = exchange.getPrincipal();
        if (principal != null) {
            System.out.println("-- PRINCIPAL --");
            System.out.println(principal);
        }

        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        if (query != null) {
            System.out.println("-- QUERY --");
            System.out.println(query);
        }
    }
}