package com.abhinav.mavendemo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private App app;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        // Port 0 automatically allocates an available ephemeral OS port
        app = new App(0);
        app.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop(0);
        }
    }

    @Test
    void testMessageReturnsValidContent() {
        String msg = App.message();
        assertNotNull(msg, "Message should not be null");
        assertTrue(msg.contains("Hello World!"), "Message must contain 'Hello World!'");
        assertTrue(msg.contains("operational"), "Message must indicate operational status");
    }

    @Test
    void testResolvePortWithExplicitArgument() {
        assertEquals(9090, App.resolvePort(new String[]{"--port=9090"}));
        assertEquals(7070, App.resolvePort(new String[]{"7070"}));
    }

    @Test
    void testResolvePortWithInvalidInputFallsBack() {
        assertEquals(App.DEFAULT_PORT, App.resolvePort(new String[]{"--port=invalid"}));
        assertEquals(App.DEFAULT_PORT, App.resolvePort(new String[]{}));
        assertEquals(App.DEFAULT_PORT, App.resolvePort(null));
    }

    @Test
    void testRootEndpointReturnsSuccessJson() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + app.getPort() + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertTrue(response.body().contains("\"service\": \"maven-cicd-demo\""));
        assertTrue(response.body().contains("\"status\": \"UP\""));
        assertTrue(response.body().contains("Hello World!"));
    }

    @Test
    void testHealthEndpointForProbes() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + app.getPort() + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\": \"UP\""));
        assertTrue(response.body().contains("\"uptime_seconds\""));
    }

    @Test
    void testHealthzAliasEndpoint() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + app.getPort() + "/healthz"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\": \"UP\""));
    }

    @Test
    void testInfoEndpointReturnsRuntimeDiagnostics() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + app.getPort() + "/api/info"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"java_version\""));
        assertTrue(response.body().contains("\"memory_mb\""));
        assertTrue(response.body().contains("\"cores\""));
    }

    @Test
    void testMethodNotAllowedOnPost() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + app.getPort() + "/"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("Method Not Allowed"));
    }
}
