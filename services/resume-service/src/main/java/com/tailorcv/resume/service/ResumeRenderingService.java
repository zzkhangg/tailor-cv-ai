package com.tailorcv.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tailorcv.resume.dto.CanonicalResume;
import com.tailorcv.resume.entity.TailoringRunEntity;
import com.tailorcv.resume.exception.ApiException;
import com.tailorcv.resume.repository.TailoringRunRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class ResumeRenderingService {
    private final TailoringRunRepository runs;
    private final ObjectMapper mapper;
    private final TemplateEngine templates;

    public ResumeRenderingService(TailoringRunRepository runs, ObjectMapper mapper, TemplateEngine templates) {
        this.runs = runs;
        this.mapper = mapper;
        this.templates = templates;
    }

    public String renderHtml(UUID runId) {
        CanonicalResume resume = successfulResume(runId);
        Context context = new Context();
        context.setVariable("resume", resume);
        try {
            return templates.process("tailored-resume", context);
        } catch (RuntimeException error) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Resume preview could not be rendered.");
        }
    }

    public byte[] renderPdf(UUID runId) {
        String html = renderHtml(runId);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            return output.toByteArray();
        } catch (IOException | RuntimeException error) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Resume PDF could not be generated.");
        }
    }

    private CanonicalResume successfulResume(UUID runId) {
        TailoringRunEntity run = runs.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tailoring run not found."));
        if (run.status != TailoringRunEntity.Status.SUCCEEDED || run.tailoredJson == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Tailoring run does not have a completed result.");
        }
        try {
            return mapper.treeToValue(run.tailoredJson, CanonicalResume.class);
        } catch (JsonProcessingException error) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored resume could not be read.");
        }
    }
}
