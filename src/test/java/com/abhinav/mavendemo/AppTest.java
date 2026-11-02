package com.abhinav.mavendemo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void messageReturnsHelloWorld() {
        assertEquals("Hello World!", App.message());
    }
}
