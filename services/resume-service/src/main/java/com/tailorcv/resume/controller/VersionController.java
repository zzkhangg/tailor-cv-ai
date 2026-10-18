package com.tailorcv.resume.controller;

import com.tailorcv.resume.dto.VersionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VersionController {
    @GetMapping("/version")
    public VersionResponse version() {
        return new VersionResponse("demo-v2");
    }
}
