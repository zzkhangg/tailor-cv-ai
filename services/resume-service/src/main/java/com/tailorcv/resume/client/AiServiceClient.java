package com.tailorcv.resume.client;

import java.net.SocketTimeoutException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownContentTypeException;

@Component("aiService")
public class AiServiceClient implements HealthIndicator {
    private final RestClient client;

    public AiServiceClient(@Value("${ai.base-url}") String baseUrl,
                           @Value("${ai.connect-timeout-ms}") int connectTimeoutMs,
                           @Value("${ai.read-timeout-ms}") int readTimeoutMs) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public void checkHealth() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.get().uri("/health").retrieve().body(Map.class);
            if (response == null || !"UP".equals(response.get("status"))) {
                throw new AiServiceException(502, "Invalid AI service health response");
            }
        } catch (AiServiceException e) {
            throw e;
        } catch (UnknownContentTypeException e) {
            throw new AiServiceException(502, "Invalid AI service health response", e);
        } catch (RestClientException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    throw new AiServiceException(504, "AI service timed out", e);
                }
                if (cause instanceof HttpMessageConversionException) {
                    throw new AiServiceException(502, "Invalid AI service health response", e);
                }
                cause = cause.getCause();
            }
            throw new AiServiceException(503, "AI service unavailable", e);
        }
    }

    @Override
    public Health health() {
        try {
            checkHealth();
            return Health.up().build();
        } catch (AiServiceException e) {
            return Health.down().build();
        }
    }
}
