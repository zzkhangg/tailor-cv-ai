package com.tailorcv.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorcv.resume.client.AiServiceClient;
import com.tailorcv.resume.dto.CanonicalResume;
import com.tailorcv.resume.dto.ResumeDetailResponse;
import com.tailorcv.resume.dto.ResumeUploadResponse;
import com.tailorcv.resume.entity.ResumeEntity;
import com.tailorcv.resume.exception.ApiException;
import com.tailorcv.resume.repository.ResumeRepository;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeWorkflowService {
    private final PdfExtractionService pdfs;
    private final AiServiceClient ai;
    private final ResumeRepository resumes;
    private final ObjectMapper mapper;
    private final Validator validator;

    public ResumeWorkflowService(PdfExtractionService pdfs, AiServiceClient ai, ResumeRepository resumes,
                                 ObjectMapper mapper, Validator validator) {
        this.pdfs = pdfs;
        this.ai = ai;
        this.resumes = resumes;
        this.mapper = mapper;
        this.validator = validator;
    }

    public ResumeUploadResponse upload(MultipartFile file) {
        var extracted = pdfs.extract(file);
        CanonicalResume parsed = ai.parse(extracted.text());
        if (parsed == null || !validator.validate(parsed).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Parser returned an invalid resume.");
        }
        var entity = new ResumeEntity();
        entity.id = UUID.randomUUID();
        entity.filename = extracted.filename();
        entity.contentType = "application/pdf";
        entity.originalPdf = extracted.bytes();
        entity.rawText = extracted.text();
        entity.parsedJson = mapper.valueToTree(parsed);
        entity.createdAt = OffsetDateTime.now();
        resumes.save(entity);
        return new ResumeUploadResponse(entity.id, entity.rawText, parsed);
    }

    public ResumeDetailResponse get(UUID id) {
        var entity = resumes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resume not found."));
        CanonicalResume parsed;
        try {
            parsed = mapper.treeToValue(entity.parsedJson, CanonicalResume.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored resume could not be read.");
        }
        return new ResumeDetailResponse(entity.id, entity.filename, entity.contentType, parsed, entity.createdAt);
    }
}
