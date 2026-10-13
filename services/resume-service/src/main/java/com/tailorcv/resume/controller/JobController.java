package com.tailorcv.resume.controller;

import com.tailorcv.resume.dto.JobRequest;
import com.tailorcv.resume.dto.JobResponse;
import com.tailorcv.resume.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobs;

    public JobController(JobService jobs) { this.jobs = jobs; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@Valid @RequestBody JobRequest request) { return jobs.create(request); }
}
