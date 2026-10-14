package com.tailorcv.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorcv.resume.client.AiServiceClient;
import com.tailorcv.resume.dto.CanonicalResume;
import com.tailorcv.resume.dto.TailorAiResponse;
import com.tailorcv.resume.client.AiServiceException;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
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
    @Autowired MockMvc mvc;
    @MockitoBean AiServiceClient ai;

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

    @Test void uploadAndReadResumeThroughPublicApi() throws Exception {
        var parsed = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("", "", ""),
                "", java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
        when(ai.parse(anyString())).thenReturn(parsed);
        var file = new MockMultipartFile("file", "fictional.pdf", "application/pdf", PdfExtractionServiceTest.pdf("Jane Doe"));
        var response = mvc.perform(multipart("/api/v1/resumes").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parsed_resume.name").value("Jane Doe"))
                .andReturn().getResponse();
        var id = java.util.UUID.fromString(mapper.readTree(response.getContentAsString()).get("resume_id").asText());
        mvc.perform(get("/api/v1/resumes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed_resume.name").value("Jane Doe"))
                .andExpect(jsonPath("$.original_pdf").doesNotExist());
        assertArrayEquals(file.getBytes(), resumes.findById(id).orElseThrow().originalPdf);
    }

    @Test void createJobWithValidation() throws Exception {
        var response = mvc.perform(post("/api/v1/jobs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Software Engineer\",\"description\":\"Build reliable Java and Python services.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        var id = UUID.fromString(mapper.readTree(response.getContentAsString()).get("job_id").asText());
        assertEquals("Software Engineer", jobs.findById(id).orElseThrow().title);
        mvc.perform(post("/api/v1/jobs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/jobs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"one word                         \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void tailorPersistsResultAndExposesItByRunId() throws Exception {
        var source = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("jane@example.com", "", ""),
                "Software engineer.", java.util.List.of("Java", "Python"), java.util.List.of(),
                java.util.List.of(), java.util.List.of());
        var tailored = new CanonicalResume("Jane Doe", source.contact(), source.summary(),
                java.util.List.of("Python", "Java"), source.experience(), source.projects(), source.education());
        var resume = new ResumeEntity();
        resume.id = UUID.randomUUID();
        resume.filename = "fictional.pdf";
        resume.contentType = "application/pdf";
        resume.originalPdf = new byte[] {1};
        resume.rawText = "Fictional resume";
        resume.parsedJson = mapper.valueToTree(source);
        resume.createdAt = OffsetDateTime.now();
        resumes.saveAndFlush(resume);
        var job = new JobDescriptionEntity();
        job.id = UUID.randomUUID();
        job.description = "Build reliable Python and Java services for a team.";
        job.createdAt = OffsetDateTime.now();
        jobs.saveAndFlush(job);
        when(ai.tailor(any(CanonicalResume.class), anyString())).thenReturn(
                new TailorAiResponse(tailored, java.util.List.of("Reordered skills."),
                        java.util.List.of("Kubernetes")));

        String body = mapper.writeValueAsString(java.util.Map.of("resume_id", resume.id, "job_id", job.id));
        var response = mvc.perform(post("/api/v1/tailor").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.tailored_resume.skills[0]").value("Python"))
                .andReturn().getResponse();
        UUID id = UUID.fromString(mapper.readTree(response.getContentAsString()).get("run_id").asText());
        mvc.perform(get("/api/v1/tailor/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.missing_requirements[0]").value("Kubernetes"));
        mvc.perform(get("/api/v1/tailor/{id}/preview", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Jane Doe")));
        mvc.perform(get("/api/v1/tailor/{id}/pdf", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsByteArray().length > 100));
        assertEquals(TailoringRunEntity.Status.SUCCEEDED, runs.findById(id).orElseThrow().status);
    }

    @Test void previewRejectsUnknownAndPendingRuns() throws Exception {
        mvc.perform(get("/api/v1/tailor/{id}/preview", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        var resume = new ResumeEntity();
        resume.id = UUID.randomUUID();
        resume.filename = "fictional.pdf";
        resume.contentType = "application/pdf";
        resume.originalPdf = new byte[] {1};
        resume.rawText = "Fictional resume";
        resume.parsedJson = mapper.valueToTree(new CanonicalResume("Jane Doe",
                new CanonicalResume.Contact("", "", ""), "", java.util.List.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of()));
        resume.createdAt = OffsetDateTime.now();
        resumes.saveAndFlush(resume);
        var job = new JobDescriptionEntity();
        job.id = UUID.randomUUID();
        job.description = "Build reliable Java services for a small product team.";
        job.createdAt = OffsetDateTime.now();
        jobs.saveAndFlush(job);
        var pending = new TailoringRunEntity();
        pending.id = UUID.randomUUID();
        pending.resume = resume;
        pending.jobDescription = job;
        pending.status = TailoringRunEntity.Status.PENDING;
        pending.createdAt = OffsetDateTime.now();
        runs.saveAndFlush(pending);

        mvc.perform(get("/api/v1/tailor/{id}/preview", pending.id))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/tailor/{id}/pdf", pending.id))
                .andExpect(status().isConflict());
    }

    @Test void failedTailoringPersistsFailedRun() throws Exception {
        var source = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("", "", ""),
                "", java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
        var resume = new ResumeEntity();
        resume.id = UUID.randomUUID();
        resume.filename = "fictional.pdf";
        resume.contentType = "application/pdf";
        resume.originalPdf = new byte[] {1};
        resume.rawText = "Fictional resume";
        resume.parsedJson = mapper.valueToTree(source);
        resume.createdAt = OffsetDateTime.now();
        resumes.saveAndFlush(resume);
        var job = new JobDescriptionEntity();
        job.id = UUID.randomUUID();
        job.description = "Build reliable Python and Java services for a team.";
        job.createdAt = OffsetDateTime.now();
        jobs.saveAndFlush(job);
        when(ai.tailor(any(CanonicalResume.class), anyString()))
                .thenThrow(new AiServiceException(422, "AI result contained unsupported claims."));
        String body = mapper.writeValueAsString(java.util.Map.of("resume_id", resume.id, "job_id", job.id));
        long countBefore = runs.count();
        mvc.perform(post("/api/v1/tailor").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(countBefore + 1, runs.count());
        assertTrue(runs.findAll().stream().anyMatch(run -> run.status == TailoringRunEntity.Status.FAILED));
    }
}
