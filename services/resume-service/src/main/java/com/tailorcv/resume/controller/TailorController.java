package com.tailorcv.resume.controller;

import com.tailorcv.resume.dto.TailorRequest;
import com.tailorcv.resume.dto.TailorRunResponse;
import com.tailorcv.resume.service.TailorService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tailor")
public class TailorController {
    private final TailorService tailoring;

    public TailorController(TailorService tailoring) { this.tailoring = tailoring; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TailorRunResponse create(@Valid @RequestBody TailorRequest request) {
        return tailoring.create(request);
    }

    @GetMapping("/{runId}")
    public TailorRunResponse get(@PathVariable UUID runId) { return tailoring.get(runId); }
}
