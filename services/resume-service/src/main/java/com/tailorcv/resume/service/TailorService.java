package com.tailorcv.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorcv.resume.client.AiServiceClient;
import com.tailorcv.resume.dto.CanonicalResume;
import com.tailorcv.resume.dto.TailorAiResponse;
import com.tailorcv.resume.dto.TailorRequest;
import com.tailorcv.resume.dto.TailorRunResponse;
import com.tailorcv.resume.entity.TailoringRunEntity;
import com.tailorcv.resume.exception.ApiException;
import com.tailorcv.resume.repository.JobDescriptionRepository;
import com.tailorcv.resume.repository.ResumeRepository;
import com.tailorcv.resume.repository.TailoringRunRepository;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TailorService {
    private final ResumeRepository resumes;
    private final JobDescriptionRepository jobs;
    private final TailoringRunRepository runs;
    private final AiServiceClient ai;
    private final ObjectMapper mapper;
    private final Validator validator;

    public TailorService(ResumeRepository resumes, JobDescriptionRepository jobs,
                         TailoringRunRepository runs, AiServiceClient ai,
                         ObjectMapper mapper, Validator validator) {
        this.resumes = resumes;
        this.jobs = jobs;
        this.runs = runs;
        this.ai = ai;
        this.mapper = mapper;
        this.validator = validator;
    }

    public TailorRunResponse create(TailorRequest request) {
        var resume = resumes.findById(request.resumeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resume not found."));
        var job = jobs.findById(request.jobId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job description not found."));
        CanonicalResume source = readResume(resume.parsedJson);
        var run = new TailoringRunEntity();
        run.id = UUID.randomUUID();
        run.resume = resume;
        run.jobDescription = job;
        run.status = TailoringRunEntity.Status.PENDING;
        run.createdAt = OffsetDateTime.now();
        runs.save(run);

        try {
            TailorAiResponse result = ai.tailor(source, job.description);
            if (result == null || result.tailoredResume() == null
                    || result.changeSummary() == null || result.missingRequirements() == null
                    || !validator.validate(result.tailoredResume()).isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI service returned an invalid result.");
            }
            run.tailoredJson = mapper.valueToTree(result.tailoredResume());
            run.changeSummary = mapper.valueToTree(new SummaryData(result.changeSummary(), result.missingRequirements()));
            run.status = TailoringRunEntity.Status.SUCCEEDED;
            run.completedAt = OffsetDateTime.now();
            runs.save(run);
            return toResponse(run);
        } catch (RuntimeException error) {
            run.status = TailoringRunEntity.Status.FAILED;
            run.errorMessage = "Tailoring could not be completed.";
            run.completedAt = OffsetDateTime.now();
            runs.save(run);
            throw error;
        }
    }

    public TailorRunResponse get(UUID id) {
        var run = runs.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tailoring run not found."));
        return toResponse(run);
    }

    private CanonicalResume readResume(com.fasterxml.jackson.databind.JsonNode json) {
        try {
            return mapper.treeToValue(json, CanonicalResume.class);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored resume could not be read.");
        }
    }

    private TailorRunResponse toResponse(TailoringRunEntity run) {
        CanonicalResume tailored = run.tailoredJson == null ? null : readResume(run.tailoredJson);
        SummaryData summary = run.changeSummary == null ? new SummaryData(List.of(), List.of())
                : mapper.convertValue(run.changeSummary, SummaryData.class);
        return new TailorRunResponse(run.id, run.status, tailored, summary.changeSummary(),
                summary.missingRequirements(), run.errorMessage, run.createdAt, run.completedAt);
    }

    private record SummaryData(List<String> changeSummary, List<String> missingRequirements) {}
}
