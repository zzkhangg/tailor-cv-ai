package com.tailorcv.resume.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TailorRequest(@NotNull UUID resumeId, @NotNull UUID jobId) {}
