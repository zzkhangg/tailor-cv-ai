package com.tailorcv.resume;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.tailorcv.resume.dto.CanonicalResume;
import jakarta.validation.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalResumeTest {
    @Test void sharedFixtureMatchesJavaContract() throws Exception {
        var mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        var fixture = Files.readString(Path.of("../../docs/contracts/canonical-resume.json"));
        var resume = mapper.readValue(fixture, CanonicalResume.class);
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(validatorFactory.getValidator().validate(resume).isEmpty());
        }
        assertEquals("Jane Doe", resume.name());
        assertEquals("2025-01", resume.experience().getFirst().startDate());
    }
}
