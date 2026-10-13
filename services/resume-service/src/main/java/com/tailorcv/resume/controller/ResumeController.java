package com.tailorcv.resume.controller;

import com.tailorcv.resume.dto.ResumeDetailResponse;
import com.tailorcv.resume.dto.ResumeUploadResponse;
import com.tailorcv.resume.service.ResumeWorkflowService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {
    private final ResumeWorkflowService workflow;

    public ResumeController(ResumeWorkflowService workflow) { this.workflow = workflow; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return workflow.upload(file);
    }

    @GetMapping("/{id}")
    public ResumeDetailResponse get(@PathVariable UUID id) { return workflow.get(id); }
}
