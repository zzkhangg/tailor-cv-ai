package com.tailorcv.resume.dto;

import com.tailorcv.resume.entity.TailoringRunEntity.Status;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TailorRunResponse(UUID runId, Status status, CanonicalResume tailoredResume,
                                List<String> changeSummary, List<String> missingRequirements,
                                String errorMessage, OffsetDateTime createdAt, OffsetDateTime completedAt) {}
