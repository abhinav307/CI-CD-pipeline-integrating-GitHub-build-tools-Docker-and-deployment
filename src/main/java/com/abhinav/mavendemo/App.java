package com.abhinav.mavendemo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Production-ready microservice for the Maven CI/CD Pipeline.
 * Provides RESTful health checks, runtime diagnostics, and service metadata.
 */
public final class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    public static final String APP_NAME = "maven-cicd-demo";
    public static final String VERSION = "1.0.0";
    public static final int DEFAULT_PORT = 8080;

    private final HttpServer server;
    private final int port;
    private final Instant startTime;

    public App(int port) throws IOException {
        this.port = port;
        this.startTime = Instant.now();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        setupRoutes();
    }

    private void setupRoutes() {
        server.createContext("/", new RootHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/healthz", new HealthHandler());
        server.createContext("/api/info", new InfoHandler());
    }

    public void start() {
        server.start();
        LOGGER.info(String.format("%s v%s started on port %d", APP_NAME, VERSION, getPort()));
    }

    public void stop(int delaySeconds) {
        LOGGER.info("Stopping HTTP server...");
        server.stop(delaySeconds);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public static String message() {
        return "Hello World! CI/CD Pipeline Microservice is operational.";
    }

    public static int resolvePort(String[] args) {
        // 1. Check CLI argument --port=<number> or raw integer
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--port=")) {
                    try {
                        return Integer.parseInt(arg.substring(7).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (args.length > 0 && args[0] != null && !args[0].startsWith("-")) {
                try {
                    return Integer.parseInt(args[0].trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Check PORT environment variable (standard in Docker, Kubernetes, Cloud Run)
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        // 3. Check system property -Dserver.port=<number>
        String propPort = System.getProperty("server.port");
        if (propPort != null && !propPort.isBlank()) {
            try {
                return Integer.parseInt(propPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        return DEFAULT_PORT;
    }

    /**
     * Root endpoint handler: returns application summary and service message.
     */
    public class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method Not Allowed\"}", "application/json");
                return;
            }

            String json = String.format(
                "{\"service\": \"%s\", \"version\": \"%s\", \"status\": \"UP\", \"message\": \"%s\", \"timestamp\": \"%s\"}",
                APP_NAME, VERSION, message(), Instant.now()
            );
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * Health check endpoint: returns UP status for Docker & Kubernetes health probes.
     */
    public class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long uptimeSeconds = Math.max(0, Instant.now().getEpochSecond() - startTime.getEpochSecond());
            String json = String.format(
                "{\"status\": \"UP\", \"service\": \"%s\", \"uptime_seconds\": %d, \"timestamp\": \"%s\"}",
                APP_NAME, uptimeSeconds, Instant.now()
            );
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * Diagnostic endpoint: returns memory, CPU, and Java runtime telemetry.
     */
    public class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Runtime rt = Runtime.getRuntime();
            long maxMb = rt.maxMemory() / (1024 * 1024);
            long totalMb = rt.totalMemory() / (1024 * 1024);
            long freeMb = rt.freeMemory() / (1024 * 1024);
            long usedMb = totalMb - freeMb;
            int availableProcessors = rt.availableProcessors();

            String json = String.format(
                "{\"service\": \"%s\", \"version\": \"%s\", \"java_version\": \"%s\", \"os\": \"%s\", \"cores\": %d, \"memory_mb\": {\"used\": %d, \"total\": %d, \"max\": %d}, \"timestamp\": \"%s\"}",
                APP_NAME,
                VERSION,
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                availableProcessors,
                usedMb,
                totalMb,
                maxMb,
                Instant.now()
            );
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) {
        // CLI one-shot execution mode if requested
        if (args != null && args.length > 0 && ("--cli".equalsIgnoreCase(args[0]) || "-c".equalsIgnoreCase(args[0]))) {
            System.out.println(message());
            return;
        }

        int port = resolvePort(args);
        try {
            App app = new App(port);
            app.start();

            System.out.printf("==================================================%n");
            System.out.printf("  %s v%s is running%n", APP_NAME, VERSION);
            System.out.printf("  Root Endpoint:   http://localhost:%d/%n", app.getPort());
            System.out.printf("  Health Check:    http://localhost:%d/health%n", app.getPort());
            System.out.printf("  Runtime Info:    http://localhost:%d/api/info%n", app.getPort());
            System.out.printf("==================================================%n");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Graceful shutdown initiated...");
                app.stop(1);
            }));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start microservice on port " + port, e);
            System.exit(1);
        }
    }
}
