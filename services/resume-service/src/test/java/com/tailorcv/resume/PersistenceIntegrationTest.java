package com.tailorcv.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorcv.resume.entity.JobDescriptionEntity;
import com.tailorcv.resume.entity.ResumeEntity;
import com.tailorcv.resume.entity.TailoringRunEntity;
import com.tailorcv.resume.repository.JobDescriptionRepository;
import com.tailorcv.resume.repository.ResumeRepository;
import com.tailorcv.resume.repository.TailoringRunRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class PersistenceIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ResumeRepository resumes;
    @Autowired JobDescriptionRepository jobs;
    @Autowired TailoringRunRepository runs;
    @Autowired ObjectMapper mapper;

    @Test void migrationAndEntitiesRoundTrip() {
        var resume = new ResumeEntity();
        resume.id = UUID.randomUUID();
        resume.filename = "fictional.pdf";
        resume.contentType = "application/pdf";
        resume.originalPdf = new byte[] {1, 2};
        resume.rawText = "Fictional test resume";
        resume.parsedJson = mapper.createObjectNode().put("name", "Jane Doe");
        resume.createdAt = OffsetDateTime.now();
        resumes.saveAndFlush(resume);

        var job = new JobDescriptionEntity();
        job.id = UUID.randomUUID();
        job.description = "Test role";
        job.createdAt = OffsetDateTime.now();
        jobs.saveAndFlush(job);

        var run = new TailoringRunEntity();
        run.id = UUID.randomUUID();
        run.resume = resume;
        run.jobDescription = job;
        run.status = TailoringRunEntity.Status.PENDING;
        run.createdAt = OffsetDateTime.now();
        runs.saveAndFlush(run);

        assertEquals("Jane Doe", resumes.findById(resume.id).orElseThrow().parsedJson.get("name").asText());
        assertEquals(TailoringRunEntity.Status.PENDING, runs.findById(run.id).orElseThrow().status);
    }
}
