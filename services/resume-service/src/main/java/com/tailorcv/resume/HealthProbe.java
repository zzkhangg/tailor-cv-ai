package com.tailorcv.resume;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class HealthProbe {
    public static void main(String[] args) throws Exception {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:8080/actuator/health/readiness"))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            if (client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode() != 200) {
                System.exit(1);
            }
        }
    }
}
