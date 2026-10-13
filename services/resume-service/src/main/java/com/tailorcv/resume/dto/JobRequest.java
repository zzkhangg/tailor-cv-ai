package com.tailorcv.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobRequest(@Size(max = 200) String title,
                         @NotBlank @Size(min = 30, max = 20000) String description) {}
