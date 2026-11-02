package com.abhinav.mavendemo;

/** Entry point for the small application packaged and deployed by the CI/CD pipeline. */
public final class App {
    private App() {
        // Utility class; do not instantiate.
    }

    public static String message() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        System.out.println(message());
    }
}
