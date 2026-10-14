package com.tailorcv.resume.client;

import com.tailorcv.resume.dto.CanonicalResume;
import com.tailorcv.resume.dto.TailorAiResponse;
import java.net.SocketTimeoutException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownContentTypeException;

@Component("aiService")
public class AiServiceClient implements HealthIndicator {
    private final RestClient client;
    private final RestClient processingClient;

    @Autowired
    public AiServiceClient(@Value("${ai.base-url}") String baseUrl,
                           @Value("${ai.connect-timeout-ms}") int connectTimeoutMs,
                           @Value("${ai.read-timeout-ms}") int readTimeoutMs,
                           @Value("${ai.processing-read-timeout-ms:45000}") int processingReadTimeoutMs) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        var processingFactory = new SimpleClientHttpRequestFactory();
        processingFactory.setConnectTimeout(connectTimeoutMs);
        processingFactory.setReadTimeout(processingReadTimeoutMs);
        this.processingClient = RestClient.builder().baseUrl(baseUrl).requestFactory(processingFactory).build();
    }

    // Kept for direct unit construction; Spring uses the configured four-argument constructor.
    public AiServiceClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        this(baseUrl, connectTimeoutMs, readTimeoutMs, 45000);
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

    public CanonicalResume parse(String text) {
        try {
            CanonicalResume result = processingClient.post().uri("/internal/parse")
                    .body(Map.of("text", text)).retrieve().body(CanonicalResume.class);
            if (result == null) throw new AiServiceException(502, "Parser returned an empty resume.");
            return result;
        } catch (AiServiceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 422) {
                throw new AiServiceException(422, "Resume text could not be parsed.", e);
            }
            if (e.getStatusCode().value() == 504) {
                throw new AiServiceException(504, "AI service timed out.", e);
            }
            if (e.getStatusCode().value() == 502) {
                throw new AiServiceException(502, "Parser returned an invalid resume.", e);
            }
            throw new AiServiceException(503, "AI service unavailable", e);
        } catch (RestClientException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    throw new AiServiceException(504, "AI service timed out", e);
                }
                if (cause instanceof HttpMessageConversionException) {
                    throw new AiServiceException(502, "Parser returned an invalid resume.", e);
                }
                cause = cause.getCause();
            }
            throw new AiServiceException(503, "AI service unavailable", e);
        }
    }

    public TailorAiResponse tailor(CanonicalResume resume, String jobDescription) {
        try {
            TailorAiResponse result = processingClient.post().uri("/internal/tailor")
                    .body(Map.of("resume", resume, "job_description", jobDescription))
                    .retrieve().body(TailorAiResponse.class);
            if (result == null) throw new AiServiceException(502, "AI service returned an empty result.");
            return result;
        } catch (AiServiceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 422) throw new AiServiceException(422, "AI result contained unsupported claims.", e);
            if (status == 504) throw new AiServiceException(504, "AI service timed out.", e);
            if (status == 502) throw new AiServiceException(502, "AI service returned an invalid result.", e);
            throw new AiServiceException(503, "AI tailoring is unavailable.", e);
        } catch (RestClientException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    throw new AiServiceException(504, "AI service timed out.", e);
                }
                if (cause instanceof HttpMessageConversionException) {
                    throw new AiServiceException(502, "AI service returned an invalid result.", e);
                }
                cause = cause.getCause();
            }
            throw new AiServiceException(503, "AI tailoring is unavailable.", e);
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
