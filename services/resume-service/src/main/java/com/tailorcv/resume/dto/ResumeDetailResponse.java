package com.tailorcv.resume.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResumeDetailResponse(UUID resumeId, String filename, String contentType,
                                   CanonicalResume parsedResume, OffsetDateTime createdAt) {}
