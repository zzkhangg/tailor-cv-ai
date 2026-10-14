package com.tailorcv.resume.controller;

import com.tailorcv.resume.dto.TailorRequest;
import com.tailorcv.resume.dto.TailorRunResponse;
import com.tailorcv.resume.service.TailorService;
import com.tailorcv.resume.service.ResumeRenderingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tailor")
public class TailorController {
    private final TailorService tailoring;
    private final ResumeRenderingService rendering;

    public TailorController(TailorService tailoring, ResumeRenderingService rendering) {
        this.tailoring = tailoring;
        this.rendering = rendering;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TailorRunResponse create(@Valid @RequestBody TailorRequest request) {
        return tailoring.create(request);
    }

    @GetMapping("/{runId}")
    public TailorRunResponse get(@PathVariable UUID runId) { return tailoring.get(runId); }

    @GetMapping(value = "/{runId}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable UUID runId) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8))
                .body(rendering.renderHtml(runId));
    }

    @GetMapping(value = "/{runId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID runId) {
        byte[] pdf = rendering.renderPdf(runId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("tailorcv-" + runId + ".pdf").build().toString())
                .body(pdf);
    }
}
