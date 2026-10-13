package com.tailorcv.resume.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(UUID jobId, String title, String description, OffsetDateTime createdAt) {}
